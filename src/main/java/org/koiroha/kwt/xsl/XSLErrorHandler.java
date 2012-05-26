/* **************************************************************************
 * Copyright (C) 2008 BJoRFUAN. All Right Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * BSD License, and comes with NO WARRANTY.
 *
 *                                                 torao <torao@bjorfuan.com>
 *                                                       http://www.moyo.biz/
 * $Id: XSLErrorHandler.java,v 1.1 2009/04/16 19:30:59 torao Exp $
*/
package org.koiroha.kwt.xsl;

import java.util.logging.Level;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// XSLErrorHandler: エラーハンドラ
// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/**
 * XML 解析中に発生した DTD 検証エラーの処理を行います。
 * <p>
 * @version $Revision: 1.1 $ $Date: 2009/04/16 19:30:59 $
 * @author torao
 * @since 2009/04/15 Java SE 6
 */
final class XSLErrorHandler extends DefaultHandler {

	// ======================================================================
	// ログ出力先
	// ======================================================================
	/**
	 * このクラスのログ出力先です。
	 * <p>
	 */
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(XSLErrorHandler.class.getName());

	// ======================================================================
	// ログ出力レベル
	// ======================================================================
	/**
	 * ログ出力のレベルです。
	 * <p>
	 */
	private final Level level;

	// ======================================================================
	// 例外発生フラグ
	// ======================================================================
	/**
	 * 解析時に受けたエラーのコールバックで例外を発生させるかどうかのフラグです。
	 * <p>
	 */
	private final boolean failOnError;

	// ======================================================================
	// SYSTEM ID
	// ======================================================================
	/**
	 * 例外に SYSTEM ID が付けられていなかった場合に使用する SYSTEM ID です。
	 * <p>
	 */
	private final String systemId;

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * エラー発生時のログ出力レベルと例外を発生させるかどうかを指定して構築を行います。
	 * <p>
	 * @param systemId SYSTEM ID
	 * @param level エラー発生時のログ出力レベル
	 * @param failOnError エラーを例外として扱う場合 true
	 */
	public XSLErrorHandler(String systemId, Level level, boolean failOnError) {
		this.systemId = systemId;
		this.level = level;
		this.failOnError = failOnError;
		return;
	}

	// ======================================================================
	// 警告の通知
	// ======================================================================
	/**
	 * 解析時の警告通知を受けます。
	 * <p>
	 * @param ex 例外
	 * @throws SAXException 警告をエラーとして扱う場合
	*/
	@Override
	public void warning(SAXParseException ex) throws SAXException {
		logger.finest("warning(" + ex + ")");
		logger.log(level, getMessage(ex));
		if(failOnError){
			throw ex;
		}
		return;
	}

	// ======================================================================
	// エラーの通知
	// ======================================================================
	/**
	 * 解析時のエラー通知を受けます。
	 * <p>
	 * @param ex 例外
	 * @throws SAXException 警告をエラーとして扱う場合
	*/
	@Override
	public void error(SAXParseException ex) throws SAXException {
		logger.finest("error(" + ex + ")");
		logger.log(level, getMessage(ex));
		if(failOnError){
			throw ex;
		}
		return;
	}

	// ======================================================================
	// エラーの通知
	// ======================================================================
	/**
	 * 解析時のエラー通知を受けます。
	 * <p>
	 * @param ex 例外
	 * @throws SAXException 警告をエラーとして扱う場合
	*/
	@Override
	public void fatalError(SAXParseException ex) throws SAXException {
		logger.finest("fatalError(" + ex + ")");
		logger.log(level, getMessage(ex));
		if(failOnError){
			throw ex;
		}
		return;
	}

	// ======================================================================
	// ログメッセージの参照
	// ======================================================================
	/**
	 * ログ出力用のメッセージを参照します。
	 * <p>
	 * @param ex 例外
	 * @return ログメッセージ
	*/
	private String getMessage(SAXParseException ex) {
		StringBuilder buffer = new StringBuilder();
		if(ex.getSystemId() != null){
			buffer.append(ex.getSystemId());
		} else {
			buffer.append(systemId);
		}
		if(ex.getLineNumber() > 0){
			buffer.append('(').append(ex.getLineNumber());
			if(ex.getColumnNumber() > 0){
				buffer.append(',').append(ex.getColumnNumber());
			}
			buffer.append(')');
		}
		buffer.append(": ").append(ex.getMessage());
		return buffer.toString();
	}

}
