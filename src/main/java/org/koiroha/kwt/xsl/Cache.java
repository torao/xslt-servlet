/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: Cache.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.xsl;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.*;
import java.util.zip.GZIPOutputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;

import org.w3c.dom.*;
import org.xml.sax.*;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// Cache: キャッシュ
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * XSL 変換結果をキャッシュし、依存ファイルの更新を監視するためのキャッシュクラスです。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/11 Java SE 6
 */
final class Cache implements Serializable {

	// ======================================================================
	// シリアルバージョン
	// ======================================================================
	/**
	 * このクラスのシリアルバージョンです。
	 * <p>
	 */
	private static final long serialVersionUID = 1L;

	// ======================================================================
	// ログ出力先
	// ======================================================================
	/**
	 * このクラスのログ出力先です。
	 * <p>
	 */
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Cache.class.getName());

	// ======================================================================
	// Web アプリケーションルートの URI
	// ======================================================================
	/**
	 * Web アプリケーションのルートを示す URI です。ログを相対パスで出力するためだけに使用します。
	 * <p>
	 */
	private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

	// ======================================================================
	// スタティックイニシャライザ
	// ======================================================================
	/**
	 *
	 * <p>
	 */
	static {
		TRANSFORMER_FACTORY.setErrorListener(new XSLErrorListener());
	}

	// ======================================================================
	// 設定
	// ======================================================================
	/**
	 * 設定です。
	 * <p>
	 */
	private final Config config;

	// ======================================================================
	// Web アプリケーションルートの URI
	// ======================================================================
	/**
	 * Web アプリケーションのルートを示す URI です。ログを相対パスで出力するためだけに使用します。
	 * <p>
	 */
	private final URI docroot;

	// ======================================================================
	// XML ファイル
	// ======================================================================
	/**
	 * このインスタンスが示すソースとなる XML のファイルです。
	 * <p>
	 */
	private final Dependency xml;

	// ======================================================================
	// 依存関係
	// ======================================================================
	/**
	 * XML ファイルが依存している別のファイルです。
	 * <p>
	 */
	private List<Dependency> dependency = new ArrayList<Dependency>();

	// ======================================================================
	// 変換済みファイル
	// ======================================================================
	/**
	 * XSL 変換済みの出力結果ファイルです。値が null になることはありませんがファイルが存在し
	 * ない可能性があります。
	 * <p>
	 */
	private final File cache;

	// ======================================================================
	// 変換済みファイル
	// ======================================================================
	/**
	 * XSL 変換済みファイルの GZIP 圧縮版です。
	 * <p>
	 */
	private final File cacheGZ;

	// ======================================================================
	// Content-Type
	// ======================================================================
	/**
	 * XSL 変換後の Content-Type です。
	 * <p>
	 */
	private String contentType = "text/html";

	// ======================================================================
	// 最終確認日時
	// ======================================================================
	/**
	 * このキャッシュが示すファイルの更新日時を最後に確認した日時です。ファイルの更新日時参照は
	 * I/O が発生するため頻繁なアクセスを抑止するために使用されます。
	 * <p>
	 */
	private long lastAccess = -1;

	// ======================================================================
	// 変換パラメータ
	// ======================================================================
	/**
	 * XSL 変換に使用するパラメータです。
	 * <p>
	 */
	private final Map<String,String> param;

	// ======================================================================
	// ドキュメントビルダーファクトリ
	// ======================================================================
	/**
	 * このインスタンスが使用するドキュメントビルダーファクトリです。
	 * <p>
	 */
	private final DocumentBuilderFactory documentBuilderFactory;

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * コンストラクタは何も行いません。
	 * <p>
	 * @param config 設定
	 * @param docroot ドキュメントルート
	 * @param contextPath コンテキストパス
	 * @param uri ソース XML の URL
	 * @param param 変換パラメータ
	 * @throws IOException 構築に失敗した場合
	 * @throws SAXException XML の解析に失敗した場合
	 */
	public Cache(Config config, String contextPath, URI docroot, URI uri, Map<String,String> param) throws IOException, SAXException{
		logger.finest("creating cache space: " + docroot.relativize(uri) + " (" + param + ")");
		this.config = config;

		// 対象 XML ファイルのドキュメントルートに対する相対パスを取得
		URI relative = docroot.relativize(uri);
		String relativePath = contextPath + File.separator +
			relative.toString().replace('/', File.separatorChar);

		// キャッシュファイルを決定
		File baseDir = Config.getCacheDirectory(config.getTempdir(), "transform");
		String cacheFilePath = baseDir.getAbsolutePath() + File.separator + relativePath;
		File cacheFile = new File(cacheFilePath);
		cacheFile = changeExtension(cacheFile, ".html");
		logger.finest("cache file: " + cacheFile);

		this.docroot = docroot;
		this.cache = cacheFile;
		this.cacheGZ = new File(cacheFile.getParent(), cacheFile.getName() + ".gz");
		this.xml = new Dependency(uri);
		this.param = new HashMap<String, String>(param);

		// ディレクトリの作成
		this.cache.getParentFile().mkdirs();

		ErrorHandler eh = config.getDTDValidationErrorHandler("http://dummy");
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
		this.documentBuilderFactory.setValidating(eh != null);
		this.documentBuilderFactory.setNamespaceAware(true);
		this.documentBuilderFactory.setXIncludeAware(true);

		// 変換処理の実行
		compile(dependency);
		return;
	}

	// ======================================================================
	// Content-Type の参照
	// ======================================================================
	/**
	 * 変換結果の Content-Type を参照します。
	 * <p>
	 * @return Content-Type
	 */
	public String getContentType(){
		return contentType;
	}

	// ======================================================================
	// 更新の判定
	// ======================================================================
	/**
	 * このキャッシュの内容が指定された日時から更新があったかどうかを判定します。
	 * <p>
	 * @param tm 判定する日時
	 * @return 判定されている場合 true
	 */
	public boolean isModifiedSince(long tm){

		// キャッシュが有効でなければ更新されていると判断
		if(! isCacheValid()){
			return true;
		}

		// キャッシュファイルの更新日時と比較
		return (cache.isFile() && cache.lastModified() > tm);
	}

	// ======================================================================
	// コンパイル済みファイルの参照
	// ======================================================================
	/**
	 * このキャッシュの変換済み HTML ファイルを参照します。ファイルが更新され
	 * ていた場合は再変換を行います。
	 * <p>
	 * @param compress GZIP 圧縮済みファイルを参照する場合 true
	 * @return 変換済み HTMLファイル
	 * @throws IOException 構築に失敗した場合
	 * @throws SAXException XML の解析に失敗した場合
	 */
	public File getCompiledFile(boolean compress) throws IOException, SAXException{

		// 依存ファイルが更新されていたら再構築
		if(! isCacheValid()){
			List<Dependency> depend = new ArrayList<Dependency>();
			compile(depend);
			this.dependency = depend;
		} else {
			logger.finest("all dependencies are valid, cache available");
		}

		// 圧縮版を要求されている場合
		if(compress){
			assert(config.isUseCompression());
			return cacheGZ;
		}

		// 非圧縮版を要求されている場合
		return cache;
	}

	// ======================================================================
	// キャッシュの削除
	// ======================================================================
	/**
	 * このキャッシュを削除します。
	 * <p>
	 */
	public void delete(){
		cache.delete();
		cacheGZ.delete();
		logger.fine("cache file removed: " + cache);
		return;
	}

	// ======================================================================
	// キャッシュファイルの有効性確認
	// ======================================================================
	/**
	 * キャッシュされている変換済み HTML ファイルの有効性を確認します。
	 * <p>
	 * @return キャッシュが有効な場合 true
	 */
	private boolean isCacheValid(){

		// 以前の確認から時間がたっていない場合
		long tm = System.currentTimeMillis();
		if(tm - lastAccess <= 5 * 1000){
			return true;
		}

		// 依存先のうち一つでも更新されていればキャッシュは無効
		lastAccess = tm;
		List<Dependency> src = dependency;
		for(int i=0; i<src.size(); i++){
			Dependency s = src.get(i);
			boolean modified = s.isModified();
			if(modified){
				logger.fine("modification detected: " + docroot.relativize(s.getURI()));
				return false;
			} else {
				logger.finest("unmodified: " + docroot.relativize(s.getURI()));
			}
		}

		// キャッシュファイルが削除されていないことを確認
		if(! cache.isFile() || ! cacheGZ.isFile()){
			logger.finest("cache file removed");
			return false;
		}

		return true;
	}

	// ======================================================================
	// 変換済みファイルの生成
	// ======================================================================
	/**
	 * 変換済みファイルを新しく生成します。
	 * <p>
	 * @param dependency 変換の依存性を格納するリターンバッファ
	 * @throws IOException 構築に失敗した場合
	 * @throws SAXException
	 */
	private void compile(Collection<Dependency> dependency) throws IOException, SAXException{
		logger.finest("start cache transformation");
		long start = System.currentTimeMillis();
		dependency.add(xml);

		// 変換対象の XML ドキュメントを読み込み
		URI uri = xml.getURI();
		logger.finest("reading xml file...: " + uri);
		Document doc = readDocument(uri);
		doc.setDocumentURI(uri.toString());

		// XML スキーマ検証の実行
		if(config.getXMLSchemaValidationErrorHandler(uri.toString()) != null){
			logger.finest("validating xml schema...: " + uri);
			validateXmlSchema(doc, uri);
		} else {
			logger.finest("skipping xml schema validation");
		}

		// XInclude によるドキュメントの依存先を取得
		// ※parse="text" で取り込まれた内容には xml:base が付けられないため再解析で検出する事にした
		jointDependency(uri, dependency,
				"http://www.w3.org/2001/XInclude", "include");

		// 変換ハンドラによる DOM 変換処理の実行
		for(TransformationHandler h: config.getTransformerHandlers()){
			doc = h.process(doc, docroot, uri, dependency);
		}
		logger.finest("finish to call transformation handler");

		// 処理対象の XML ドキュメントから XSL スタイルシートの URI を取得
		URI stylesheet = getStylesheet(doc);
		if(stylesheet == null){
			logger.finest("xml stylesheet is not specified: " + uri);
			transform(doc, null);		// ※無指定の場合は恒等変換で出力
			return;
		}

		// スタイルシートが相対 URI の場合は絶対 URI に変換
		if(! stylesheet.isAbsolute()){
			String path = stylesheet.toString();
			if(path.startsWith("/")){
				// "/" から始まる場合はドキュメントルートからの相対パスとみなす
				do{
					path = path.substring(1);
				} while(path.startsWith("/"));
				stylesheet = docroot.resolve(path);
			} else {
				stylesheet = uri.resolve(stylesheet);
			}
		}
		dependency.add(new Dependency(stylesheet));
		logger.finest("xsl stylesheet: " + stylesheet);

		// XSL ファイル内で参照している全ての URI を取得
		jointDependency(stylesheet, dependency,
				"http://www.w3.org/1999/XSL/Transform", "import", "include");

		// 変換処理を実行して依存性をリセット
		transform(doc, stylesheet);
		for(Dependency dep: dependency){
			dep.reset();
		}

		// 結果のログ出力
		logger.fine("xsl transformation complete: " + (cache.length()/1024) + "kB: " + (System.currentTimeMillis() - start) + "ms: " + docroot.relativize(uri));
		if(logger.isLoggable(Level.FINEST)){
			StringBuilder buffer = new StringBuilder();
			for(Dependency dep: dependency){
				buffer.append(", ");
				buffer.append(docroot.relativize(dep.getURI()));
			}
			logger.finest("depend " + dependency.size() + " files" + buffer);
		}
		return;
	}

	// ======================================================================
	// XSL URI の参照
	// ======================================================================
	/**
	 * 指定されたドキュメント中の処理命令を参照し XSL ファイルの URI を参照します。ドキュメント
	 * 中に処理命令が存在しない場合や "text/xsl" 型でない場合は null を返します。
	 * <p>
	 * @param doc XSL ファイルを参照するドキュメント
	 * @return XSL ファイルの URI
	 */
	private URI getStylesheet(Document doc) {
		NodeList nl = doc.getChildNodes();
		for(int i=0; i<nl.getLength(); i++){

			// 処理命令でない場合はスキップ
			if(! (nl.item(i) instanceof ProcessingInstruction)){
				continue;
			}

			// xml-stylesheet でない場合はスキップ
			ProcessingInstruction pi = (ProcessingInstruction)nl.item(i);
			if(! pi.getTarget().equals("xml-stylesheet")){
				logger.finest("not a xml-stylesheet: " + pi.getTarget());
				continue;
			}

			// text/xsl 以外のタイプであればスキップ
			Pattern pattern = Pattern.compile("type\\s*=\\s*[\"\']text/xsl[\"\']");
			Matcher matcher = pattern.matcher(pi.getData());
			if(! matcher.find()){
				logger.finest("type is not text/xsl: " + pi.getData());
				continue;
			}

			// href の指定がなければスキップ
			pattern = Pattern.compile("href\\s*=\\s*[\"\']([^\"\']*)[\"\']");
			matcher = pattern.matcher(pi.getData());
			if(! matcher.find()){
				logger.finest("href not found: " + pi.getTarget());
				continue;
			}

			return URI.create(matcher.group(1));
		}

		// デフォルトのスタイルシート URI を参照
		return config.getDefaultXSLURI();
	}

	// ======================================================================
	// XInclude による参照先の解析
	// ======================================================================
	/**
	 * 指定された XML ドキュメントに含まれている XInclude 要素から依存先 URI を参照します。
	 * <p>
	 * @param base XInclude を含むドキュメントの URI
	 * @param dependency 取り込んだ依存先の格納先
	 * @param xmlns 依存先要素の名前空間 URI
	 * @param localNames 依存先要素のローカル名
	 * @throws IOException 変換に失敗した場合
	 */
	private void jointDependency(URI base, Collection<Dependency> dependency, String xmlns, String... localNames) throws IOException{

		Set<URI> depend = new HashSet<URI>();
		DependencyCapture capture = new DependencyCapture(base, depend, xmlns, localNames);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		factory.setXIncludeAware(false);
		try{
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(capture);
			reader.setEntityResolver(config.getSchemaCatalog());
			reader.parse(base.toString());
		} catch(Exception ex){
			throw new IllegalStateException(ex);
		}

		// 依存性を設定
		for(URI uri: depend){
			Dependency dep = new Dependency(uri);
			dependency.add(dep);
		}

		return;
	}

	// ======================================================================
	// ドキュメントの読み込み
	// ======================================================================
	/**
	 * 指定された URI から XML ドキュメントを読み込みます。
	 * <p>
	 * @param uri ドキュメントの URI
	 * @return ドキュメント
	 * @throws IOException 変換に失敗した場合
	 */
	private Document readDocument(URI uri) throws IOException{


		Document doc = null;
		try{
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			logger.finest("reading xml: " + uri + " (" +
				"namespace=" + builder.isNamespaceAware() + "," +
				"xinclude=" + builder.isXIncludeAware() + "," +
				"validating=" + builder.isValidating() + ")");
			builder.setEntityResolver(config.getSchemaCatalog());

			// DTD 検証用のエラーハンドラの設定
			ErrorHandler eh = config.getDTDValidationErrorHandler(uri.toString());
			if(eh != null){
				builder.setErrorHandler(eh);
			} else {
				logger.finest("skipping dtd validation");
			}

			InputSource is = new InputSource(uri.toURL().toString());
			doc = builder.parse(is);
		} catch(IOException ex){
			throw ex;
		} catch(Exception ex){
			throw new IOException(ex);
		}
		return doc;
	}


	// ======================================================================
	// キャッシュの作成
	// ======================================================================
	/**
	 * XSL 変換処理を実行しキャッシュを生成します。
	 * <p>
	 * @param doc ドキュメント
	 * @param stylesheet XSL
	 * @throws IOException 変換に失敗した場合
	 */
	private void transform(Document doc, URI stylesheet) throws IOException{
		OutputStream out = null;
		InputStream in = null;
		GZIPOutputStream zout = null;

		try{

			// 非圧縮版を作成
			cache.getParentFile().mkdirs();
			out = new BufferedOutputStream(new FileOutputStream(cache));
			transform(out, doc, stylesheet, param);
			if(stylesheet == null){
				contentType = "text/xml";
			}
			out.close();

			// GZIP 圧縮版を作成
			if(config.isUseCompression()){
				zout = new GZIPOutputStream(new FileOutputStream(cacheGZ));
				in = new FileInputStream(cache);
				byte[] buffer = new byte[1024];
				while(true){
					int len = in.read(buffer);
					if(len < 0)	break;
					zout.write(buffer, 0, len);
				}
				in.close();
				zout.finish();
				zout.close();
			}

			// 変換に使用した XML も作成
			if(config.isKeepTransformedXML()){
				File file = new File(cache.getAbsolutePath() + ".xml");
				out = new BufferedOutputStream(new FileOutputStream(file));
				transform(out, doc, null, param);
				out.close();
			}

		} finally {
			try{
				if(out != null)	out.close();
			} catch(IOException ex){/* */}
			try{
				if(in != null)	in.close();
			} catch(IOException ex){/* */}
			try{
				if(zout != null)	zout.close();
			} catch(IOException ex){/* */}
		}
		return;
	}


	// ======================================================================
	// 変換処理の実行
	// ======================================================================
	/**
	 * 指定されたドキュメントを XSL 変換して出力ストリームへ出力します。スタイルシートに null
	 * を指定した場合はドキュメントの内容をそのまま XML として出力します。
	 * <p>
	 * @param out 出力先のストリーム
	 * @param doc ドキュメント
	 * @param stylesheet XSL スタイルシート
	 * @param param 変換パラメータ
	 * @throws IOException 変換に失敗した場合
	 */
	private static void transform(OutputStream out, Document doc, URI stylesheet, Map<String,String> param) throws IOException{
		InputStream in = null;
		try{

			// トランスフォーマーの構築
			Transformer transformer = null;
			if(stylesheet == null){
				transformer = TRANSFORMER_FACTORY.newTransformer();
			} else {
				in = stylesheet.toURL().openStream();
				Source source = new StreamSource(in);
				source.setSystemId(stylesheet.toString());
				transformer = TRANSFORMER_FACTORY.newTransformer(source);
				in.close();
				in = null;

				// 構築に失敗している場合は例外
				if(transformer == null){
					throw new IOException(stylesheet.toString());
				}
				logger.finest("output method: " + transformer.getOutputProperty("method"));
			}

			// パラメータの設定
			if(param != null){
				for(Map.Entry<String,String> entry: param.entrySet()){
					transformer.setParameter(entry.getKey(), entry.getValue());
					logger.finest("set parameter: " + entry.getKey() + "=" + entry.getValue());
				}
			}

			// XML の変換出力
			Source source = new DOMSource(doc);
			Result result = new StreamResult(out);
			transformer.transform(source, result);

		} catch(TransformerException ex){
			throw new IOException(ex);
		} finally {
			try{
				if(in != null)	in.close();
			} catch(IOException ex){/* */}
		}

		return;
	}

	// ======================================================================
	// ファイル拡張子の変更
	// ======================================================================
	/**
	 * 指定されたファイルの拡張子を変更します。
	 * <p>
	 * @param file 拡張子を変更するファイル
	 * @param ext 新しいファイル拡張子
	 * @return 新しい拡張し (例:".html");
	 */
	private static File changeExtension(File file, String ext){
		File dir = file.getParentFile();
		String fileName = file.getName();
		int sep = fileName.lastIndexOf('.');
		if(sep >= 0){
			fileName = fileName.substring(0, sep);
		}
		return new File(dir, fileName + ext);
	}

	// ======================================================================
	// XML スキーマ検証
	// ======================================================================
	/**
	 * 指定されたドキュメントの XML スキーマ検証を行います。
	 * <p>
	 * @param doc 検証するドキュメント
	 * @param base ベースの URI
	 * @throws SAXException 中断された場合
	 */
	private void validateXmlSchema(Document doc, URI base) throws SAXException{

		// ドキュメント内で参照している全てのスキーマを取得
		Map<String,Schema> schema = new HashMap<String, Schema>();
		retrieveXmlSchema(doc.getDocumentElement(), base, schema);

		// ドキュメント検証の実行
		SchemaCatalog catalog = config.getSchemaCatalog();
		ErrorHandler eh = config.getXMLSchemaValidationErrorHandler(base.toString());
		for(Schema s: schema.values()){
			Validator validator = s.newValidator();
			validator.setErrorHandler(eh);
			validator.setResourceResolver(catalog);
			Source src = new DOMSource(doc);
			src.setSystemId(base.toString());
			try{
				validator.validate(src);
			} catch(IOException ex){
				logger.log(Level.SEVERE, "unexpected error", ex);
			}
		}
		return;
	}

	// ======================================================================
	// XML スキーマの取得
	// ======================================================================
	/**
	 * 指定された要素内で参照している XML スキーマを取得してリストに格納します。
	 * <p>
	 * @param elem XML スキーマを取得するリスト
	 * @param base 基準 URI
	 * @param schema 検出したスキーマの格納先
	 */
	private void retrieveXmlSchema(Element elem, URI base, Map<String,Schema> schema) {

		// XML Schema Instance が指定されている場合はそのロケーションから取得
		String xsi = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
		if(elem.hasAttributeNS(xsi, "schemaLocation")){
			String schemaLocation = elem.getAttributeNS(xsi, "schemaLocation");
			String[] token = schemaLocation.split("[ \t\r\n]+");
			for(int i=0; i+1<token.length; i+=2){
				Schema s = getXmlSchema(base, token[i], token[i+1]);
				if(s != null){
					schema.put(token[i], s);
				}
			}
		}

		// 名前空間が指定されてる場合は対応するスキーマをカタログから取得
		NamedNodeMap attrs = elem.getAttributes();
		for(int i=0; i<attrs.getLength(); i++){
			Attr attr = (Attr)attrs.item(i);
			if(XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())){
				String ns = attr.getValue();
				Schema s = getXmlSchema(base, ns);
				if(s != null){
					schema.put(ns, s);
				}
			}
		}

		// 再帰呼び出し
		NodeList ns = elem.getChildNodes();
		for(int i=0; i<ns.getLength(); i++){
			if(ns.item(i) instanceof Element){
				retrieveXmlSchema((Element)ns.item(i), base, schema);
			}
		}
		return;
	}

	// ======================================================================
	// スキーマの取得
	// ======================================================================
	/**
	 * 指定された URL から XML Schema を取得します。
	 * <p>
	 * @param base ベースの URI
	 * @param xmlns 名前空間 URI
	 * @param schemaUri XML スキーマの URI
	 * @return XML スキーマ
	 */
	private Schema getXmlSchema(URI base, String xmlns, String schemaUri){
		SchemaCatalog catalog = config.getSchemaCatalog();
		try{
			URL url = base.resolve(schemaUri).toURL();
			return catalog.getXmlSchema(xmlns, url.toString());
		} catch(Exception ex){
			logger.log(Level.SEVERE, "unexpected error", ex);
		}
		return null;
	}

	// ======================================================================
	// スキーマの取得
	// ======================================================================
	/**
	 * 指定された URL から XML Schema を取得します。
	 * <p>
	 * @param base ベースの URI
	 * @param xmlns 名前空間 URI
	 * @return XML スキーマ
	 */
	private Schema getXmlSchema(URI base, String xmlns){
		SchemaCatalog catalog = config.getSchemaCatalog();
		try{
			return catalog.getXmlSchema(xmlns);
		} catch(Exception ex){
			logger.log(Level.SEVERE, "unexpected error", ex);
		}
		return null;
	}

}
