/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: XSLErrorListener.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.xsl;

import javax.xml.transform.*;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// XSLErrorListener: XSL エラーリスナ
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * XSL 変換時のエラーをログ出力するためのエラーリスナです。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/11 Java SE 6
 */
final class XSLErrorListener implements ErrorListener {

	// ======================================================================
	// ログ出力先
	// ======================================================================
	/**
	 * このクラスのログ出力先です。
	 * <p>
	 */
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(XSLErrorListener.class.getName());

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * コンストラクタは何も行いません。
	 * <p>
	 */
	public XSLErrorListener() {
		return;
	}

	// ======================================================================
	// 警告通知
	// ======================================================================
	/**
	 * 警告をログ出力します。
	 * <p>
	 * @param ex XSL 変換の例外
	 */
	@Override
	public void warning(TransformerException ex) {
		logger.warning(ex.getMessageAndLocation());
		return;
	}

	// ======================================================================
	// エラー通知
	// ======================================================================
	/**
	 *
	 * <p>
	 * @param ex XSL 変換の例外
	 * @throws TransformerException
	 */
	@Override
	public void error(TransformerException ex) throws TransformerException{
		logger.severe(ex.getMessageAndLocation());
		throw ex;
	}

	// ======================================================================
	// エラー通知
	// ======================================================================
	/**
	 *
	 * <p>
	 * @param ex XSL 変換の例外
	 * @throws TransformerException
	 */
	@Override
	public void fatalError(TransformerException ex) throws TransformerException{
		logger.severe(ex.getMessageAndLocation());
		throw ex;
	}

}
