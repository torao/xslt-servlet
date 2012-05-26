/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: DependencyCapture.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.xsl;

import java.net.*;
import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// DependencyCapture: 依存先取得ハンドラ
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * ドキュメント内から依存先 URI を取得するためのハンドラです。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/14 Java SE 6
 */
final class DependencyCapture extends DefaultHandler {

	// ======================================================================
	// ログ出力先
	// ======================================================================
	/**
	 * このクラスのログ出力先です。
	 * <p>
	 */
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DependencyCapture.class.getName());

	// ======================================================================
	// 名前空間 URI
	// ======================================================================
	/**
	 * 対象の名前空間 URI です。
	 * <p>
	 */
	private final String namespaceUri;

	// ======================================================================
	// ローカル名
	// ======================================================================
	/**
	 * 対象要素のローカル名です。
	 * <p>
	 */
	private final Set<String> localNames = new HashSet<String>();

	// ======================================================================
	// ベース URI
	// ======================================================================
	/**
	 * 相対パスの基準となる URI です。
	 * <p>
	 */
	private final URI baseUri;

	// ======================================================================
	// 依存先 URI
	// ======================================================================
	/**
	 * 解析によって取り込んだ依存先 URI です。
	 * <p>
	 */
	private final Set<URI> dependUri;

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * コンストラクタは何も行いません。
	 * <p>
	 * @param base 相対パスの基準 URI
	 * @param dependUri 依存先 URI の格納先
	 * @param ns 対象の名前空間 URI
	 * @param localNames 対象のローカル名
	 */
	public DependencyCapture(URI base, Set<URI> dependUri, String ns, String... localNames) {
		this.baseUri = base;
		this.dependUri = dependUri;
		this.namespaceUri = ns;
		this.localNames.addAll(Arrays.asList(localNames));
		return;
	}

	// ======================================================================
	// 要素の開始通知
	// ======================================================================
	/**
	 * 依存先
	 * <p>
	 * @param namespace 名前空間 URI
	 * @param localName ローカル名
	 * @param name 修飾名
	 * @param attr 属性
	*/
	@Override
	public void startElement(String namespace, String localName, String name, Attributes attr) {

		// 名前空間が一致しなければ何もしない
		if(! this.namespaceUri.equals(namespace)){
			return;
		}

		// ローカル名が一致し href 属性を持つ場合
		if(this.localNames.contains(localName) && attr.getIndex("href") >= 0){
			String href = attr.getValue("href");
			try{
				URI uri = new URI(href);
				if(! uri.isAbsolute()){
					uri = baseUri.resolve(uri);
				}
				this.dependUri.add(uri);
			} catch(URISyntaxException ex){
				logger.finest("unrecognized uri: " + href + "; " + ex);
			}
		}
		return;
	}

}
