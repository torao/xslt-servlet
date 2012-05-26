/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: Config.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.xsl;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.*;

import javax.servlet.ServletException;

import org.xml.sax.ErrorHandler;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// Config: 設定クラス
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * 設定を表すクラスです。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/12 Java SE 6
 */
public final class Config implements Serializable {

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
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Config.class.getName());

	// ======================================================================
	// 設定内容
	// ======================================================================
	/**
	 * 設定の内容です。
	 * <p>
	 */
	private final Map<String,String> config = new HashMap<String, String>();

	// ======================================================================
	// 作業ディレクトリ
	// ======================================================================
	/**
	 * 作業ディレクトリです。
	 * <p>
	 */
	private final File tempdir;

	// ======================================================================
	// XML スキーマカタログ
	// ======================================================================
	/**
	 * XML スキーマカタログです。
	 * <p>
	 */
	private final SchemaCatalog catalog;

	// ======================================================================
	// 変換ハンドラ
	// ======================================================================
	/**
	 * 変換ハンドラです。
	 * <p>
	 */
	private final List<TransformationHandler> handler;

	// ======================================================================
	// デフォルト XSL ファイル
	// ======================================================================
	/**
	 * 処理対象の XML にスタイルシートが指定されていない場合に使用する XSL ファイル URI
	 * の設定名 {@value} です。
	 * 値は {@code true} または {@code false} を指定します。省略した場合は XSL 変換は
	 * 行われません。
	 * <p>
	 */
	public static final String DEFAULT_XSL_URI = "default-xsl-uri";

	// ======================================================================
	// DTD 検証失敗時の挙動
	// ======================================================================
	/**
	 * 変換対象の XML ドキュメントの DTD 検証に失敗した場合の動作を指定する設定名 {@value}
	 * です。以下の値を使用することが出来ます。
	 * <p>
	 * <ul>
	 * <li> <i>省略</i> - DTD による検証処理を行いません。</li>
	 * <li> {@code logging(<i>level</i>)} - 指定されたレベルでログに出力します。
	 * <i>level</i> には {@link Level#parse(String)} で解釈可能なレベルを指定します。
	 * </li>
	 * <li> {@code fail} - 例外を発生させ 500 Internal Server Error とします。</li>
	 * <p>
	 * DTD 検証は XML ドキュメント内に &lt;!DOCTYPE&gt; が宣言された時のみ行われます。
	 * <p>
	 */
	public static final String DTD_VALIDATION_ERROR = "dtd-validation-error";

	// ======================================================================
	// XML Schema 検証失敗時の挙動
	// ======================================================================
	/**
	 * 変換対象の XML ドキュメントの XML Schema 検証に失敗した場合の動作を指定する設定名
	 * {@value}です。以下の値を使用することが出来ます。
	 * <p>
	 * <ul>
	 * <li> <i>省略</i> - XML Schema による検証処理を行いません。</li>
	 * <li> {@code logging(<i>level</i>)} - 指定されたレベルでログに出力します。
	 * <i>level</i> には {@link Level#parse(String)} で解釈可能なレベルを指定します。
	 * </li>
	 * <li> {@code fail} - 例外を発生させ 500 Internal Server Error とします。</li>
	 * <p>
	 * XML Schema 検証は XML ドキュメント内で XML Schema Instance の schemaLocation
	 * が指定された時のみ行われます。
	 * <p>
	 */
	public static final String XML_SCHEMA_VALIDATION_ERROR = "xml-schema-validation-error";

	// ======================================================================
	// 圧縮転送使用の設定
	// ======================================================================
	/**
	 * クライアントが圧縮転送を受け入れる場合に XSL 変換結果を圧縮して転送するかを表す設定名
	 * {@value} です。
	 * 値は {@code true} または {@code false} を指定します。デフォルトは {@code true}
	 * です。
	 * <p>
	 */
	public static final String USE_COMPRESSION = "use-compression";

	// ======================================================================
	// 圧縮転送使用の設定
	// ======================================================================
	/**
	 * 変換に使用した XML を保存するかどうかを表す設定名 {@value} です。この設定は
	 * {@link TransformationHandler} によるドキュメントの加工結果をデバッグする場合に有用です。
	 * 値は {@code true} または {@code false} を指定します。デフォルトは {@code false}
	 * です。
	 * <p>
	 */
	public static final String KEEP_TRANSFORMED_XML = "keep-transformed-xml";

	// ======================================================================
	// 転送ハンドラの設定
	// ======================================================================
	/**
	 * XSL 適用前後に DOM を直接加工する {@link TransformationHandler} を表す設定名
	 * です。
	 * {@link TransformationHandler} サブクラス名をコンマ区切りで指定します。
	 * 定数 {@value} を示します。
	 * <p>
	 */
	public static final String TRANSFORMATION_HANDLERS = "transformation-handlers";

	// ======================================================================
	// スキーマカタログ
	// ======================================================================
	/**
	 * スキーマカタログの URI を示す設定名 {@value} です。
	 * <p>
	 */
	public static final String SCHEMA_CATALOG = "schema-catalog";

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * 設定内容を指定して構築を行います。
	 * <p>
	 * @param config 設定内容
	 * @param tempdir 作業ディレクトリ
	 * @param docroot ドキュメントルートの URI
	 * @throws ServletException 初期化に失敗した場合
	 */
	Config(Map<String,String> config, File tempdir, URI docroot) throws ServletException{
		this.tempdir = tempdir;
		this.config.putAll(config);

		// 変換ハンドラのロード
		List<TransformationHandler> handler = new ArrayList<TransformationHandler>();
		String param = getString(TRANSFORMATION_HANDLERS, "");
		try{
			StringTokenizer tk = new StringTokenizer(param, ", \t\r\n");
			while(tk.hasMoreTokens()){
				String className = tk.nextToken();
				handler.add((TransformationHandler)Class.forName(className).newInstance());
				logger.config("load transformation handler: " + className);
			}
		} catch(Exception ex){
			logger.log(Level.SEVERE, "fail to load transformation handler", ex);
			throw new ServletException(ex);
		}
		this.handler = Collections.unmodifiableList(handler);

		// XML スキーマカタログの取得
		param = getString(SCHEMA_CATALOG, "");
		if(param.length() > 0){
			while(param.startsWith("/")){
				param = param.substring(1);
			}
			URI uri = docroot.resolve(param);
			this.catalog = new SchemaCatalog(uri, tempdir);
		} else {
			this.catalog = new SchemaCatalog(null, tempdir);
		}

		return;
	}

	// ======================================================================
	// 設定値の参照
	// ======================================================================
	/**
	 * 指定された名前の設定値を参照します。名前に該当する値が設定されていない場合はデフォルト値を
	 * 返します。
	 * <p>
	 * @param name 設定の名前
	 * @param def デフォルト値
	 * @return 設定値
	 */
	public String getString(String name, String def){
		String value = config.get(name);
		if(value == null){
			value = def;
		}
		return value;
	}

	// ======================================================================
	// 設定値の参照
	// ======================================================================
	/**
	 * 指定された名前の設定値を参照します。名前に該当する値が設定されていない場合はデフォルト値を
	 * 返します。
	 * <p>
	 * @param name 設定の名前
	 * @param def デフォルト値
	 * @return 設定値
	 */
	public boolean getBoolean(String name, boolean def){
		String value = config.get(name);
		if(value == null){
			return def;
		}
		return Boolean.valueOf(value);
	}

	// ======================================================================
	// 作業ディレクトリの参照
	// ======================================================================
	/**
	 * 作業ディレクトリを参照します。
	 * <p>
	 * @return 作業ディレクトリ
	 */
	public File getTempdir() {
		return tempdir;
	}

	// ======================================================================
	// XML スキーマカタログの参照
	// ======================================================================
	/**
	 * XML スキーマカタログを参照します。
	 * <p>
	 * @return XML スキーマカタログ
	 */
	public SchemaCatalog getSchemaCatalog(){
		return catalog;
	}

	// ======================================================================
	// 変換ハンドラの参照
	// ======================================================================
	/**
	 * 変換ハンドラを参照します。
	 * <p>
	 * @return 変換ハンドラ
	 */
	public List<TransformationHandler> getTransformerHandlers(){
		return handler;
	}

	// ======================================================================
	// デフォルト XSL URI の参照
	// ======================================================================
	/**
	 * デフォルト XSL ファイルの URI を参照します。
	 * <p>
	 * @return デフォルトの URI
	 */
	public URI getDefaultXSLURI(){
		String uri = getString(DEFAULT_XSL_URI, null);
		if(uri == null){
			return null;
		}
		return URI.create(uri);
	}

	// ======================================================================
	// DTD 検証の参照
	// ======================================================================
	/**
	 * DTD 検証を参照します。
	 * <p>
	 * @param systemId 解析対象の SYSTEM ID
	 * @return DTD 検証を行う場合 true
	 */
	public ErrorHandler getDTDValidationErrorHandler(String systemId){
		return getErrorHandler(systemId, config.get(DTD_VALIDATION_ERROR));
	}

	// ======================================================================
	// XML Schema 検証の参照
	// ======================================================================
	/**
	 * XML Schema 検証のためのエラーハンドラを参照します。検証を行う必要がない場合は null
	 * が返されます。
	 * <p>
	 * @param systemId 解析対象の SYSTEM ID
	 * @return XML Schema 検証のためのエラーハンドラ
	 */
	public ErrorHandler getXMLSchemaValidationErrorHandler(String systemId){
		return getErrorHandler(systemId, config.get(XML_SCHEMA_VALIDATION_ERROR));
	}

	// ======================================================================
	// 圧縮の参照
	// ======================================================================
	/**
	 * 圧縮設定を参照します。
	 * <p>
	 * @return XSL 変換結果を圧縮して転送する場合 true
	 */
	public boolean isUseCompression(){
		return getBoolean(USE_COMPRESSION, true);
	}

	// ======================================================================
	// DTD 検証の参照
	// ======================================================================
	/**
	 * DTD 検証を参照します。
	 * <p>
	 * @return DTD 検証を行う場合 true
	 */
	public boolean isKeepTransformedXML(){
		return getBoolean(KEEP_TRANSFORMED_XML, false);
	}

	// ======================================================================
	// キャッシュディレクトリルートの参照
	// ======================================================================
	/**
	 * キャッシュディレクトリルートを参照します。
	 * <p>
	 * @param dir サーブレットの作業ディレクトリ
	 * @param id キャッシュディレクトリの ID
	 * @return キャッシュディレクトリルート
	 */
	public static File getCacheDirectory(File dir, String id){
		String className = Config.class.getName();
		int sep = className.lastIndexOf('.');
		String packageName = className.substring(0, sep);
		String path = packageName.replace('.', File.separatorChar);
		return new File(dir, path + File.separator + id);
	}

	// ======================================================================
	// エラーハンドラの参照
	// ======================================================================
	/**
	 * 指定された検証エラー時挙動の値に基づいてエラーハンドラを構築します。
	 * <p>
	 * @param systemId SYSTEM ID
	 * @param spec 検証エラー時の挙動を示す値
	 * @return エラーハンドラ
	 */
	private static ErrorHandler getErrorHandler(String systemId, String spec) {

		// 検証を行わない場合
		if(spec == null || spec.trim().length() == 0){
			return null;
		}
		spec = spec.trim();

		// 検証失敗時に例外を発生させる場合
		if(spec.equalsIgnoreCase("fail")){
			return new XSLErrorHandler(systemId, Level.SEVERE, true);
		}

		// レベル省略のログ出力の場合
		if(spec.equalsIgnoreCase("logging")){
			return new XSLErrorHandler(systemId, Level.WARNING, false);
		}

		// レベル指定のログ出力の場合
		Pattern pattern = Pattern.compile("logging\\s*\\(\\s*([^\\s\\)]*)\\s*\\)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(spec);
		if(matcher.matches()){
			Level level = Level.WARNING;
			try{
				if(matcher.group().length() != 0){
					level = Level.parse(matcher.group(1).toUpperCase());
				}
			} catch(IllegalArgumentException ex){
				logger.warning("unrecognizable logging level: " + matcher.group(1) + ";" +
						" use default level: " + level.getName());
			}
			return new XSLErrorHandler(systemId, level, false);
		}

		// 認識できない値が指定された場合
		logger.warning("unrecognizable validation-error spec: " + spec);
		return null;
	}

}
