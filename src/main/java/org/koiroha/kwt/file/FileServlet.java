/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: FileServlet.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.file;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import javax.servlet.*;
import javax.servlet.http.*;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// FileServlet: ファイルサーブレット
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * クライアントからのリクエストに応じたファイルを送信するためのサーブレットです。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/12 Java SE 6
 */
public class FileServlet extends HttpServlet {

	// ======================================================================
	// シリアルバージョン
	// ======================================================================
	/**
	 * このクラスのシリアルバージョンです。
	 * <p>
	 */
	private static final long serialVersionUID = 1L;

	// ======================================================================
	// 送信バッファサイズ
	// ======================================================================
	/**
	 * 送信バッファサイズです。
	 * <p>
	 */
	private int sendBufferSize = 4 * 1024;

	// ======================================================================
	// ドキュメントルート
	// ======================================================================
	/**
	 * ドキュメントルートの URI です。
	 * <p>
	 */
	protected URI docroot = null;

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * コンストラクタは何も行いません。
	 * <p>
	 */
	public FileServlet() {
		return;
	}

	// ======================================================================
	// サーブレットの初期化
	// ======================================================================
	/**
	 * このサーブレットのインスタンスを初期化します。
	 * <p>
	 * @throws ServletException サーブレットの初期化に失敗した場合
	*/
	@Override
	public void init() throws ServletException {
		super.init();

		// ドキュメントルートの取得
		try{
			docroot = getLocalURI("/", true);
		} catch(IOException ex){
			throw new ServletException(ex);
		}

		return;
	}

	// ======================================================================
	// ローカル URI の参照
	// ======================================================================
	/**
	 * 指定されたパスをドキュメントルートからの相対パスとみなして URI を参照します。パスに該当
	 * するファイルが存在しない場合は null を返します。
	 * <p>
	 * @param path URI として参照するパス
	 * @param dir ディレクトリを取得する場合 true
	 * @return パスに対する URI
	 * @throws IOException 変換に失敗した場合
	*/
	protected URI getLocalURI(String path, boolean dir) throws IOException{
		ServletContext context = getServletContext();

		// ローカルファイルとしてのパスを参照
		String realPath = context.getRealPath(path);
		if(realPath != null){
			File file = new File(realPath);
			if((! dir && file.isFile()) || (dir && file.isDirectory())){
				return file.toURI();
			}
		}

		// リソースから URI を参照
		URL url = context.getResource(path);
		if(url != null){
			try{
				return url.toURI();
			} catch(URISyntaxException ex){
				throw new IOException(ex);
			}
		}

		return null;
	}

	// ======================================================================
	// GZIP 圧縮転送の判定
	// ======================================================================
	/**
	 * 指定されたリクエストが GZIP 圧縮に対応しているかどうかを判定します。
	 * <p>
	 * @param request 判定するリクエスト
	 * @return GZIP 圧縮に対応している場合 true
	 */
	protected boolean acceptGZIPCompression(HttpServletRequest request){

		// Accept-Encoding ヘッダを参照
		String acceptEncoding = request.getHeader("Accept-Encoding");
		if(acceptEncoding == null){
			return false;
		}

		// gzip のトークンを参照
		StringTokenizer tk = new StringTokenizer(acceptEncoding, ", \t\r\n");
		while(tk.hasMoreTokens()){
			if(tk.nextToken().equalsIgnoreCase("gzip")){
				return true;
			}
		}

		return false;
	}

	// ======================================================================
	// ファイルの送信
	// ======================================================================
	/**
	 * 指定されているローカルファイルをストリームに出力します。送信対象のファイルが存在しない
	 * 場合は例外が発生します。
	 * <p>
	 * @param file ローカルファイル
	 * @param out 出力先のストリーム
	 * @param compress 出力時に圧縮を行う場合 true
	 * @throws IOException 出力に失敗した場合
	 */
	protected void send(File file, OutputStream out, boolean compress) throws IOException{

		// 圧縮を行う場合は GZIP 出力ストリームを使用して再帰呼び出し
		if(compress){
			GZIPOutputStream gout = new GZIPOutputStream(out);
			send(file, gout, false);
			gout.finish();
			return;
		}

		// ファイルの送信
		FileInputStream in = null;
		try{
			in = new FileInputStream(file);
			int size = (int)Math.min(sendBufferSize, file.length());
			byte[] buffer = new byte[size];
			while(true){
				int len = in.read(buffer);
				if(len < 0){
					break;
				}
				out.write(buffer, 0, len);
			}
			out.flush();
		} finally {
			close(in);
		}

		return;
	}

	// ======================================================================
	// ストリームのクローズ
	// ======================================================================
	/**
	 * 指定されたストリームを例外なしでクローズします。
	 * <p>
	 * @param closeable クローズするストリーム
	 */
	protected static void close(Closeable closeable){
		try{
			if(closeable != null){
				closeable.close();
			}
		} catch(IOException ex){/* */}
		return;
	}

}
