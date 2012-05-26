/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: SchemaCatalog.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.xsl;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

import javax.servlet.ServletException;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;

import org.w3c.dom.*;
import org.w3c.dom.ls.*;
import org.xml.sax.*;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// XMLSchemaCatalog: XML スキーマカタログ
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * XML のスキーマや DTD のローカルの保存場所を表すクラスです。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/11 Java SE 6
 */
public class SchemaCatalog implements EntityResolver, LSResourceResolver, Serializable {

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
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SchemaCatalog.class.getName());

	// ======================================================================
	// 名前空間 URI
	// ======================================================================
	/**
	 * スキーマカタログ XML の名前空間 URI です。
	 * <p>
	 */
	public static final String NAMESPACE_URI = "http://www.koiroha.org/xmlns/kwt/schema-catalog";

	// ======================================================================
	// DTD PUBLIC ID マップ
	// ======================================================================
	/**
	 * PUBLIC ID をキーとした DTD ファイルへのマップです。
	 * <p>
	*/
	private final Map<String,URI> publicId = new HashMap<String,URI>();

	// ======================================================================
	// DTD SYSTEM ID マップ
	// ======================================================================
	/**
	 * SYSTEM ID をキーとした DTD ファイルへのマップです。
	 * <p>
	*/
	private final Map<String,URI> systemId = new HashMap<String,URI>();

	// ======================================================================
	// XML スキーママップ
	// ======================================================================
	/**
	 * 名前空間 URI に対する XML スキーマファイルへのマップです。
	 * <p>
	*/
	private final Map<String,URI> namespace = new HashMap<String,URI>();

	// ======================================================================
	// キャッシュ用ディレクトリ
	// ======================================================================
	/**
	 * キャッシュ用のディレクトリです。
	 * <p>
	*/
	private final File dir;

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * スキーマカタログの定義を指定して構築を行います。
	 * <p>
	 * @param file スキーマカタログファイル
	 * @param dir キャッシュ用のディレクトリ
	 * @throws ServletException
	 */
	public SchemaCatalog(URI file, File dir) throws ServletException{
		this.dir = dir;

		// スキーマカタログが指定されていない場合
		if(file == null){
			logger.finer("no schema-catalog specified");
			return;
		}

		try{

			// ドキュメントのロード
			logger.finest("loading schema catalog: " + file);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file.toURL().toString());

			// DTD の定義を参照
			NodeList nl = doc.getElementsByTagNameNS(NAMESPACE_URI, "dtd");
			for(int i=0; i<nl.getLength(); i++){
				Element elem = (Element)nl.item(i);
				String location = elem.getAttribute("location");

				// PUBLIC ID の設定
				String publicId = elem.getAttribute("public-id");
				if(publicId.length() > 0){
					logger.finest("catalog public-id: " + publicId + "=" + file.resolve(location));
					this.publicId.put(publicId, file.resolve(location));
				}

				// SYSTEM ID の設定
				String systemId = elem.getAttribute("system-id");
				if(systemId.length() > 0){
					logger.finest("catalog system-id: " + systemId + "=" + file.resolve(location));
					this.systemId.put(systemId, file.resolve(location));
				}
			}

			// XML Schema の定義を参照
			nl = doc.getElementsByTagNameNS(NAMESPACE_URI, "xml-schema");
			for(int i=0; i<nl.getLength(); i++){
				Element elem = (Element)nl.item(i);
				String location = elem.getAttribute("location");

				// 名前空間 URI の設定
				String ns = elem.getAttribute("namespace-uri");
				if(ns.length() > 0){
					logger.finest("catalog namespace uri: " + ns + "=" + file.resolve(location));
					this.namespace.put(ns, file.resolve(location));
				}

				// SYSTEM ID の設定
				String systemId = elem.getAttribute("system-id");
				if(systemId.length() > 0){
					logger.finest("catalog system-id: " + systemId + "=" + file.resolve(location));
					this.systemId.put(systemId, file.resolve(location));
				}
			}

		} catch(Exception ex){
			throw new ServletException(ex);
		}
		return;
	}

	// ======================================================================
	// XML スキーマの参照
	// ======================================================================
	/**
	 * 指定された名前空間 URI に対する XML スキーマを参照します。このインスタンスのカタログに
	 * 定義されていない場合は新規に取得してローカルへキャッシュします。
	 * <p>
	 * @param namespaceUri 名前空間 URI
	 * @return XML スキーマ
	 * @throws IOException スキーマの読み込みに失敗した場合
	 * @throws SAXException スキーマのインスタンス化に失敗した場合
	 */
	public Schema getXmlSchema(String namespaceUri) throws IOException, SAXException{

		// 名前空間に対するスキーマが定義されていない場合
		if(! namespace.containsKey(namespaceUri)){
			return null;
		}

		// 指定された名前空間のスキーマがこのカタログに定義されている場合
		URI uri = namespace.get(namespaceUri);
		return loadXmlSchema(uri, uri.toString());
	}

	// ======================================================================
	// XML スキーマの参照
	// ======================================================================
	/**
	 * 指定された名前空間 URI に対する XML スキーマを参照します。このインスタンスのカタログに
	 * 定義されていない場合は新規に取得してローカルへキャッシュします。
	 * <p>
	 * @param namespaceUri 名前空間 URI
	 * @param systemId スキーマの SYSTEM ID
	 * @return XML スキーマ
	 * @throws IOException スキーマの読み込みに失敗した場合
	 * @throws SAXException スキーマのインスタンス化に失敗した場合
	 */
	public Schema getXmlSchema(String namespaceUri, String systemId) throws IOException, SAXException {

		// 指定された名前空間のスキーマがこのカタログに定義されている場合
		URI uri = null;
		if(namespace.containsKey(namespaceUri)){
			uri = namespace.get(namespaceUri);
		}

		// キャッシュ内のファイルを参照
		if(uri == null){
			File file = getLocalFile(systemId);
			if(file != null){
				uri = file.toURI();
			}
		}

		// スキーマが定義されていない場合
		if(uri == null){
			return null;
		}

		// 指定された名前空間のスキーマがこのカタログに定義されている場合
		return loadXmlSchema(uri, systemId);
	}

	// ======================================================================
	// エンティティの解決
	// ======================================================================
	/**
	 * 指定された PUBLIC ID または SYSTEM ID に対する入力ソースを参照します。
	 * <p>
	 * @param publicId PUBLIC ID
	 * @param systemId SYSTEM ID
	 * @return エンティティの入力ソース
	 * @throws IOException 入力ソースの参照に失敗した場合
	*/
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws IOException {
		logger.finest("resolveEntity(" + publicId + "," + systemId + ")");

		// SYSTEM ID 無指定やローカルのファイルを示している場合はデフォルトの動作
		if(systemId != null && systemId.toLowerCase().startsWith("file:/")){
			logger.finest("skip local file");
			return null;
		}

		InputSource is = new InputSource();
		is.setPublicId(publicId);
		is.setSystemId(systemId);

		// PUBLIC ID からローカルの DTD ファイルを参照
		URI uri = this.publicId.get(publicId);

		// SYSTEM ID からローカルの DTD ファイルを参照
		if(uri == null){
			uri = this.systemId.get(systemId);
		}

		// キャッシュ内のファイルを参照
		if(uri == null){
			File file = getLocalFile(systemId);
			if(file == null){
				return null;
			}
			uri = file.toURI();
		}

		is.setByteStream(getInputStream(uri.toURL()));
		return is;
	}

	// ======================================================================
	// リソースの解決
	// ======================================================================
	/**
	 * 指定されたリソースの入力ソースを参照します。
	 * XML スキーマを参照するために呼び出されます。
	 * <p>
	 * @param type http://www.w3.org/2001/XMLSchema
	 * @param namespaceURI http://www.w3.org/XML/1998/namespace
	 * @param publicId null
	 * @param systemId http://www.w3.org/2001/xml.xsd
	 * @param baseURI http://www.w3.org/2002/08/xhtml/xhtml1-strict.xsd
	 * @return 入力ソース
	*/
	@Override
	public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
		logger.finest("resolveResource(" + type + "," + namespaceURI + "," + publicId + "," + systemId + "," + baseURI + ")");

		// SYSTEM ID 無指定やローカルのファイルを示している場合はデフォルトの動作
		if(systemId != null && systemId.toLowerCase().startsWith("file:/")){
			logger.finest("skip local file");
			return null;
		}

		// キャッシュ内のファイルを参照
		File file = getLocalFile(systemId);
		if(file == null){
			return null;
		}

		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementationLS impl = (DOMImplementationLS)builder.getDOMImplementation();
			LSInput in = impl.createLSInput();
			in.setPublicId(publicId);
			in.setSystemId(systemId);
			in.setByteStream(new FileInputStream(file));
			return in;
		} catch(Exception ex){
			throw new IllegalStateException(ex);
		}
	}

	// ======================================================================
	// XML スキーマの構築
	// ======================================================================
	/**
	 * 指定された URI から XML スキーマを構築します。
	 * <p>
	 * @param uri スキーマの URI
	 * @param systemId スキーマの SYSTEM ID
	 * @return XML スキーマ
	 * @throws IOException スキーマの読み込みに失敗した場合
	 * @throws SAXException スキーマのインスタンス化に失敗した場合
	 */
	private Schema loadXmlSchema(URI uri, String systemId) throws IOException, SAXException{
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		factory.setResourceResolver(this);
		Schema schema = null;
		InputStream in = null;
		try{
			in = getInputStream(uri.toURL());
			Source src = new StreamSource(in);
			if(systemId != null){
				src.setSystemId(systemId);
			}
			schema = factory.newSchema(src);
		} finally {
			try{
				if(in != null)	in.close();
			} catch(IOException ex){
				logger.warning("fail to close: " + ex);
			}
		}
		return schema;
	}

	// ======================================================================
	// キャッシュファイルの参照
	// ======================================================================
	/**
	 * 指定された SYSTEM ID に対するローカルキャッシュファイル名を参照します。キャッシュ内に
	 * ファイルが存在しない場合は指定された SYSTEM ID から新規に作成します。SYSTEM ID から
	 * の内容の取得に失敗した場合は null を返します。
	 * <p>
	 * @param systemId SYSTEM ID
	 * @return 保存先のファイル
	 */
	private File getLocalFile(String systemId){

		// キャッシュ内のファイルを参照
		String name = toSafe(systemId);
		File file = new File(Config.getCacheDirectory(dir, "schema"), name);
		if(file.isFile()){
			return file;
		}

		// 存在しない場合はローカルに保存してファイルを返す
		try{
			save(systemId, file);
		} catch(IOException ex){
			logger.warning("fail to retrieve resource: " + systemId + ": " + ex);
			return null;
		}
		return file;
	}

	// ======================================================================
	// ローカルキャッシュの作成
	// ======================================================================
	/**
	 * 指定された SYSTEM ID に対するローカルキャッシュを作成します。
	 * <p>
	 * @param systemId SYSTEM ID
	 * @param file 保存先のファイル
	 * @throws IOException ファイルの取得または保存に失敗した場合
	 */
	private void save(String systemId, File file) throws IOException{
		logger.finest("save(" + systemId + "," + file + ")");

		// 保存先がキャッシュディレクトリ以外でない事を保証
		if(! file.getCanonicalPath().startsWith(dir.getCanonicalPath())){
			throw new IllegalStateException("invalid file location to write: " + file);
		}

		// SYSTEM ID の示すファイルをローカルに保存
		file.getParentFile().mkdirs();
		URL url = new URL(systemId);
		OutputStream out = null;
		InputStream in = null;
		byte[] buffer = new byte[1024];
		try{
			in = getInputStream(url);
			out = new FileOutputStream(file);
			while(true){
				int len = in.read(buffer);
				if(len < 0)	break;
				out.write(buffer, 0, len);
			}
		} finally {
			try{
				if(in != null)	in.close();
			} catch(IOException ex){/* */}
			try{
				if(out != null)	out.close();
			} catch(IOException ex){/* */}
		}

		logger.fine("save resource to local: " + file.getName() + " (" + (file.length() / 1024) + "kB)");
		return;
	}

	// ======================================================================
	// 入力ストリームの参照
	// ======================================================================
	/**
	 * 指定された URL に対する入力ストリームを参照します。
	 * <p>
	 * @param url URL
	 * @return 入力ストリーム
	 * @throws IOException 入力ストリームの参照に失敗した場合
	 */
	private static final InputStream getInputStream(URL url) throws IOException{
		URLConnection con = url.openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1)");
		con.setAllowUserInteraction(false);
		return con.getInputStream();
	}

	// ======================================================================
	// 安全な文字列へ変換
	// ======================================================================
	/**
	 * 指定された文字列をファイルシステムで使用できる安全な文字列に変換します。
	 * <p>
	 * @param systemId ファイルシステムで安全に使用できる文字列に変換する名前
	 * @return ファイルとして使用できる文字列
	 */
	private static final String toSafe(String systemId){
		if(systemId.startsWith("http://")){
			systemId = systemId.substring("http://".length());
		}

		byte[] binary = systemId.getBytes(Charset.forName("UTF-8"));
		StringBuilder buffer = new StringBuilder();
		for(int i=0; i<binary.length; i++){
			byte ch = binary[i];
			if((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')
			|| ch == '-' || ch == '.'){
				buffer.append((char)ch);
			} else if(ch == '_'){
				buffer.append("__");
			} else if(ch == ' '){
				buffer.append("_+");
			} else if(ch == '/'){
				buffer.append("_");
			} else {
				buffer.append('_');
				buffer.append(Character.forDigit((ch >>  4) & 0x0F, 16));
				buffer.append(Character.forDigit((ch >>  0) & 0x0F, 16));
			}
		}
		String safeName = buffer.toString();
		logger.finest(systemId + " -> " + safeName);
		return buffer.toString();
	}

}
