/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: Dependency.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.xsl;

import java.io.*;
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// Dependency: 依存先ファイルクラス
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * 依存先のファイルを追跡するためのクラスです。ファイルの更新検知で依存先を追跡するために使用します。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/11 Java SE 6
 */
public final class Dependency implements Serializable{

	// ======================================================================
	// シリアルバージョン
	// ======================================================================
	/**
	 * このクラスのシリアルバージョンです。
	 * <p>
	 */
	private static final long serialVersionUID = 1L;

	// ======================================================================
	// 依存先 URI
	// ======================================================================
	/**
	 * 依存先を示す URI です。
	 * <p>
	 */
	private final URI uri;

	// ======================================================================
	// 依存先ファイル
	// ======================================================================
	/**
	 * {@link #uri} がローカルファイルを示す場合のファイルです。
	 * <p>
	 */
	private final File file;

	// ======================================================================
	// ファイル更新日時
	// ======================================================================
	/**
	 * {@link #uri} がローカルファイルを示している場合にそのファイルから最後に読み取りを行った
	 * 時点でのファイル更新日時です。
	 * <p>
	 */
	private long lastModified = -1;

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * 依存先の URI を指定して構築を行います。
	 * <p>
	 * @param uri 依存先の URI
	 */
	public Dependency(URI uri) {
		assert(uri.isAbsolute());
		this.uri = uri;

		// ローカルファイルを参照
		String scheme = uri.getScheme();
		if(! scheme.equalsIgnoreCase("file")){
			this.file = null;
		} else {
			String path = uri.getPath();
			path = path.replace('/', File.separatorChar);
			this.file = new File(path);
		}

		return;
	}

	// ======================================================================
	// 依存先 URI の参照
	// ======================================================================
	/**
	 * 依存先の URI を参照します。
	 * <p>
	 * @return 依存先 URI
	 */
	public URI getURI(){
		return uri;
	}

	// ======================================================================
	// 最終更新日時のリセット
	// ======================================================================
	/**
	 * {@link #isModified()} メソッドで更新判定に使用する日時を現在のファイルの更新日時に
	 * リセットします。
	 * <p>
	 */
	public void reset(){
		if(file == null){
			lastModified = 0;
		} else {
			lastModified = file.lastModified();
		}
		return;
	}

	// ======================================================================
	// ファイル更新の判定
	// ======================================================================
	/**
	 * 最後に {@link #reset()} が呼び出された時点からこのインスタンスが示すファイルが更新さ
	 * れているかどうかを判定します。
	 * <p>
	 * @return ファイルが更新されている場合 true
	 */
	public boolean isModified(){
		if(file == null){
			return false;
		}
		return (file.lastModified() != lastModified);
	}

	// ======================================================================
	// インスタンスの文字列化
	// ======================================================================
	/**
	 * このインスタンスを文字列化します。
	 * <p>
	 * @return インスタンスの文字列
	 */
	@Override
	public String toString(){
		return uri.toString() + "[" + DateFormat.getDateTimeInstance().format(new Date(lastModified)) + "]";
	}

}
