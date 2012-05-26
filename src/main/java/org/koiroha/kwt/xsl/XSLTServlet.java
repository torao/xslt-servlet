/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: XSLTServlet.java,v 1.2 2009/04/16 20:29:24 torao Exp $
*/
package org.koiroha.kwt.xsl;

import java.io.*;
import java.net.URI;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;

import javax.servlet.*;
import javax.servlet.http.*;

import org.koiroha.kwt.file.FileServlet;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// XSLServlet: XSL サーブレット
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * XML ファイルに対してサーバサイドで XSL 変換処理を行うためのサーブレットです。利用可能な
 * サーブレットパラメータは {@link Config} クラスを参照してください。
 * <p>
 * @version $Revision: 1.2 $ $Date: 2009/04/16 20:29:24 $
 * @author torao
 * @since 2009/04/11 Java SE 6
 */
public class XSLTServlet extends FileServlet {

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
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(XSLTServlet.class.getName());

	// ======================================================================
	// キャッシュ
	// ======================================================================
	/**
	 * 処理対象 XML ファイルのパス情報に対するキャッシュです。
	 * <p>
	 */
	private final Map<String,Cache> cache = Collections.synchronizedMap(new HashMap<String,Cache>());

	// ======================================================================
	// サーブレット設定
	// ======================================================================
	/**
	 * XSL 変換処理の設定です。
	 * <p>
	 */
	private Config config = null;

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * コンストラクタは何も行いません。
	 * <p>
	 */
	public XSLTServlet() {
		return;
	}

	// ======================================================================
	// サーブレットの初期化
	// ======================================================================
	/**
	 * サーブレットを初期化します。
	 * <p>
	 * @throws ServletException 初期化に失敗した場合
	*/
	@Override
	public void init() throws ServletException {
		super.init();

		// 作業ディレクトリの取得
		ServletContext context =  getServletContext();
		File tempdir = (File)context.getAttribute("javax.servlet.context.tempdir");
		if(tempdir == null){
			tempdir = new File(System.getProperty("java.io.tmpdir", "."));
		}
		logger.finest("temporary directory: " + tempdir);

		// サーブレットパラメータから設定を構築
		@SuppressWarnings("unchecked")
		Enumeration<String> en = (Enumeration<String>)getInitParameterNames();
		Map<String,String> params = new HashMap<String, String>();
		while(en.hasMoreElements()){
			String name = en.nextElement();
			String value = getInitParameter(name);
			params.put(name, value);
			logger.finest("servlet parameter: " + name + "=" + value);
		}
		this.config = new Config(params, tempdir, docroot);

		return;
	}

	// ======================================================================
	// GET の実行
	// ======================================================================
	/**
	 * GET を実行します。
	 * <p>
	 * @param req リクエスト
	 * @param res レスポンス
	 * @throws ServletException
	 * @throws IOException
	*/
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		try{

			// リクエストされたファイルに対する URI を参照
			String pathInfo = req.getServletPath();
			URI uri = getLocalURI(pathInfo, false);

			// 該当するファイルが存在しない場合
			if(uri == null){

				// キャッシュから削除
				Cache cache = this.cache.remove(pathInfo);
				if(cache != null){
					cache.delete();
				}

				logger.finest("return " + HttpServletResponse.SC_NOT_FOUND + " not found response: " + pathInfo);
				res.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			// キャッシュを参照
			Cache cache = this.cache.get(pathInfo);
			if(cache == null){
				Map<String,String> param = new HashMap<String, String>();
				param.put("schema", req.getScheme());
				param.put("server-name", req.getServerName());
				param.put("server-port", String.valueOf(req.getServerPort()));
				param.put("context-path", req.getContextPath());
				param.put("path-info", pathInfo);
				cache = new Cache(config, req.getContextPath(), docroot, uri, param);
				this.cache.put(pathInfo, cache);
			}

			// If-Modified-Since 付きの場合は最終更新日時と比較
			long modifiedSince = req.getDateHeader("If-Modified-Since");
			if(modifiedSince > 0){
				logger.finest("if-modified-since specified: "
						+ DateFormat.getDateTimeInstance().format(new Date(modifiedSince)));
				if(! cache.isModifiedSince(modifiedSince)){
					res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					logger.finer("not modified");
					return;
				}
				logger.finest("requested uri modified");
			}

			// GZIP 圧縮対応の判定
			boolean useCompress = (config.isUseCompression() && acceptGZIPCompression(req));

			// ヘッダの設定
			File file = cache.getCompiledFile(useCompress);
			res.setContentType(cache.getContentType());
			res.setHeader("Content-Length", Long.toString(file.length()));
			if(useCompress){
				res.setHeader("Content-Encoding", "gzip");
				logger.finest("gzip compressed response");
			}

			// 変換済みファイル内容の送信
			send(file, res.getOutputStream(), false);

			logger.finest("finish xsl servlet");
		} catch(Exception ex){
			logger.log(Level.SEVERE, ex.toString(), ex);
			res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		return;
	}

}
