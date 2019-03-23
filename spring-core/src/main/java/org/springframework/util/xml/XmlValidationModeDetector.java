/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.xml;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Detects whether an XML stream is using DTD- or XSD-based validation.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XmlValidationModeDetector {

	/**
	 * Indicates that the validation should be disabled.
	 *
	 * 指明禁用验证
	 */
	public static final int VALIDATION_NONE = 0;

	/**
	 * Indicates that the validation mode should be auto-guessed, since we cannot find
	 * a clear indication (probably choked on some special characters, or the like).
	 *
	 * 这段话是说，验证模式应该是自动猜测出来的，即程序判断验证模式的类型，但可能出现程序判断不了的情况，如果是程序自动判断出来的，就是用这个值
	 */
	public static final int VALIDATION_AUTO = 1;

	/**
	 * Indicates that DTD validation should be used (we found a "DOCTYPE" declaration).
	 *
	 * 指明 使用 DTD 验证模式（在文档中发现了一个 "DOCTYPE" 声明）
	 */
	public static final int VALIDATION_DTD = 2;

	/**
	 * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
	 *
	 * 指明 使用 XSD 验证模式（在文档中发现了一个 "DOCTYPE" 声明）, 这里和上面一样，但是dtd优先，发现了 doctype 声明就认为是dtd，最终发现不了 doctype才认为是xsd
	 */
	public static final int VALIDATION_XSD = 3;


	/**
	 * The token in a XML document that declares the DTD to use for validation
	 * and thus that DTD validation is being used.
	 *
	 * 这个符号声明在文档中，声明使用DTD验证，而且DTD验证正在使用，翻译的不好，意思就是说xml文档中有这个字符串，应该使用DTD验证
	 */
	private static final String DOCTYPE = "DOCTYPE";

	/**
	 * The token that indicates the start of an XML comment.
	 *
	 * 表示XML注释开始的标记
	 */
	private static final String START_COMMENT = "<!--";

	/**
	 * The token that indicates the end of an XML comment.
	 *
	 * 表示XML注释结束的标记
	 */
	private static final String END_COMMENT = "-->";


	/**
	 * Indicates whether or not the current parse position is inside an XML comment.
	 *
	 * 指示当前解析位置是否在XML注释内,只有当一行行读取xml文档时，返现了 START_COMMENT 代表字符串，才会被置为true
	 */
	private boolean inComment;


	/**
	 * Detect the validation mode for the XML document in the supplied {@link InputStream}.
	 * Note that the supplied {@link InputStream} is closed by this method before returning.
	 * @param inputStream the InputStream to parse
	 * @throws IOException in case of I/O failure
	 * @see #VALIDATION_DTD
	 * @see #VALIDATION_XSD
	 *
	 * 对参数InputStream检测xml文档的验证模式
	 * 需要注意，这个方法返回前会关闭参数提供的InputStream
	 *
	 * 这个方法就是找 xml文档注释的开始符号，找到了，就判断后面是不是有 "DOCTYPE",有就返回dtd，否则返回xsd,异常就返回auto
	 */
	public int detectValidationMode(InputStream inputStream) throws IOException {
		// Peek into the file to look for DOCTYPE.
		// 把inputStream 封装成BufferedReader
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			boolean isDtdValidated = false;
			String content;
			// 循环一行一行读取
			while ((content = reader.readLine()) != null) {
				//
				content = consumeCommentTokens(content);
				// 如果 this.inComment==true 或者 content不包含文字了（null 或 空串 或 全是空格），就继续循环
				if (this.inComment || !StringUtils.hasText(content)) {
					continue;
				}
				// 如果content包含"DOCTYPE"就中断
				if (hasDoctype(content)) {
					// 把isDtdValidated置为true
					isDtdValidated = true;
					break;
				}
				// 如果循环到标签了("<"后面是个字母),就中断
				if (hasOpeningTag(content)) {
					// End of meaningful data...
					break;
				}
			}
			// 根据isDtdValidated判断返回dtd还是xsd
			return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
		}
		catch (CharConversionException ex) {
			// Choked on some character encoding...
			// Leave the decision up to the caller.
			// 异常了返回auto
			return VALIDATION_AUTO;
		}
		finally {
			// 关闭流
			reader.close();
		}
	}


	/**
	 * Does the content contain the DTD DOCTYPE declaration?
	 */
	private boolean hasDoctype(String content) {
		return content.contains(DOCTYPE);
	}

	/**
	 * Does the supplied content contain an XML opening tag. If the parse state is currently
	 * in an XML comment then this method always returns false. It is expected that all comment
	 * tokens will have consumed for the supplied content before passing the remainder to this method.
	 */
	private boolean hasOpeningTag(String content) {
		if (this.inComment) {
			return false;
		}
		int openTagIndex = content.indexOf('<');
		return (openTagIndex > -1 && (content.length() > openTagIndex + 1) &&
				Character.isLetter(content.charAt(openTagIndex + 1)));
	}

	/**
	 * Consumes all the leading comment data in the given String and returns the remaining content, which
	 * may be empty since the supplied content might be all comment data. For our purposes it is only important
	 * to strip leading comment content on a line since the first piece of non comment content will be either
	 * the DOCTYPE declaration or the root element of the document.
	 */
	@Nullable
	private String consumeCommentTokens(String line) {
		// 检测当前这行是不是包含 xml注释开始符号 "<!--"  或 xml注释结束符号 "-->"
		// 如果都不包含，就返回这行数据
		if (!line.contains(START_COMMENT) && !line.contains(END_COMMENT)) {
			return line;
		}
		// 如果包含
		String currLine = line;
		// 把截掉一部分的字符串重新赋值给currLine，如果新的currLine不是null
		while ((currLine = consume(currLine)) != null) {
			// 再判断下新的currLine去前后空格后是不是以 "<!--" 开头，如果是就返回currLine，否则继续循环截串，然后判断，直到返回或当前这行被截完
			if (!this.inComment && !currLine.trim().startsWith(START_COMMENT)) {
				return currLine;
			}
		}
		// 如果当前这行被截完了都没发现目标，就返回null
		return null;
	}

	/**
	 * Consume the next comment token, update the "inComment" flag
	 * and return the remaining content.
	 */
	@Nullable
	private String consume(String line) {
		// this.inComment 默认值是false(没指定，在对象初始化时给默认值)，
		// 所以第一次会调用startComment(line)，之后，如果line中包含字符串 "<!--" this.inComment会被置为true
		int index = (this.inComment ? endComment(line) : startComment(line));
		// 如果返回不是-1，就从index处截取字符串，也就是截取出 "<!--" 或 "-->" 的长度后开始截串，而后返回
		return (index == -1 ? null : line.substring(index));
	}

	/**
	 * Try to consume the {@link #START_COMMENT} token.
	 * @see #commentToken(String, String, boolean)
	 */
	private int startComment(String line) {
		// 如果字符串line中包含 xml注释开始符号 "<!--" 就将 this.inComment 设为 true，返回token的长度减1
		return commentToken(line, START_COMMENT, true);
	}

	private int endComment(String line) {
		// 如果字符串line中包含 xml注释结束符号 "-->" 就将 this.inComment 设为 false，返回token的长度减1
		return commentToken(line, END_COMMENT, false);
	}

	/**
	 * Try to consume the supplied token against the supplied content and update the
	 * in comment parse state to the supplied value. Returns the index into the content
	 * which is after the token or -1 if the token is not found.
	 */
	private int commentToken(String line, String token, boolean inCommentIfPresent) {
		// 检测 字符串line中是不是包含字符串 token
		int index = line.indexOf(token);
		if (index > - 1) {
			// 如果包含，就把inCommentIfPresent赋值给this.inComment
			this.inComment = inCommentIfPresent;
		}
		// 返回token的长度减1
		return (index == -1 ? index : index + token.length());
	}

}
