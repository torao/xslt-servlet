/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: TransformationHandler.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.xsl;

import java.net.URI;
import java.util.Collection;

import org.w3c.dom.Document;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// TransformationHandler: 変換ハンドラ
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * XSL 変換処理前の XML 文書に対する操作を行うためのインターフェースです。このインスタンスは
 * 全てのドキュメントに対して XSL 変換前に呼び出されます。XSL のみでは実現不可能な文書操作を
 * 行うことを目的としています。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/11 Java SE 6
 */
public interface TransformationHandler {

	// ======================================================================
	// XML 文書の操作
	// ======================================================================
	/**
	 * 指定された XML 文書に対する変換処理を行います。
	 * <p>
	 * 外部ファイルを使用して XML 文書の加工を行う必要がある場合、その外部ファイルを「依存先」
	 * として <i>depend</i> コレクションに追加することでファイルを更新監視対象に加えることが
	 * 出来ます。
	 * <p>
	 * 返値は実際の変換に使用するドキュメントとして使用されます。一般的にはサブクラスによって加工
	 * された <i>doc</i> を想定していますが、ドキュメントそのものを別の物に置き換える事も
	 * できます。メソッドは null を返すことは出来ません。
	 * <p>
	 * @param doc 前処理を行うドキュメント
	 * @param docroot ドキュメントルートの URI
	 * @param uri ドキュメントのローカル URI
	 * @param depend ドキュメント依存性の追加先
	 * @return 処理を行ったドキュメント
	*/
	public Document process(Document doc, URI docroot, URI uri, Collection<Dependency> depend);

}
