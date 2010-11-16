/*******************************************************************************
 * Copyright (c) 2010 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.internal.javascript.ti;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.VariableStatement;
import org.eclipse.dltk.javascript.typeinfo.IModelBuilder;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;

/**
 * Implements support for javadocs tags .
 * 
 * @see http://jsdoc.sourceforge.net/
 * @see http://code.google.com/p/jsdoc-toolkit/wiki/TagType
 * @see http://code.google.com/p/jsdoc-toolkit/wiki/TagParam
 */
public class JSDocSupport implements IModelBuilder {

	public static String[] getTags() {
		return new String[] { DEPRECATED, PARAM_TAG, TYPE_TAG, RETURNS_TAG,
				PRIVATE_TAG };
	}

	private static final String PRIVATE_TAG = "@private";

	public void processMethod(FunctionStatement statement, IMethod method) {
		if (statement.getDocumentation() == null) {
			return;
		}
		final String comment = statement.getDocumentation().getText();
		if (method.getType() == null) {
			parseType(method, comment);
		}
		parseParams(method, comment);

		parseDeprecation(method, comment);

		parsePrivate(method, comment);
	}

	/**
	 * @param method
	 * @param comment
	 */
	private void parsePrivate(IMember member, final String comment) {
		if (comment.indexOf(PRIVATE_TAG) != -1) {
			member.setPrivate(true);
		}
	}

	public void processVariable(VariableStatement statement, IVariable variable) {
		if (statement.getDocumentation() == null) {
			return;
		}
		final String comment = statement.getDocumentation().getText();
		if (variable.getType() == null) {
			parseType(variable, comment);
		}
		parseDeprecation(variable, comment);

		parsePrivate(variable, comment);
	}

	private static final String DEPRECATED = "@deprecated"; //$NON-NLS-1$

	private void parseDeprecation(IMember member, String comment) {
		int index = comment.indexOf(DEPRECATED);
		if (index != -1) {
			member.setDeprecated(true);
		}
	}

	private static final String PARAM_TAG = "@param"; //$NON-NLS-1$

	private void parseParams(IMethod method, String comment) {
		int index = comment.indexOf(PARAM_TAG);
		Map<String, Type> objectPropertiesTypes = new HashMap<String, Type>();

		while (index != -1) {
			int endLineIndex = comment.indexOf("\n", index);
			if (endLineIndex == -1) {
				endLineIndex = comment.length();
			}
			String parameterString = comment.substring(
					index + PARAM_TAG.length(), endLineIndex);
			StringTokenizer st = new StringTokenizer(parameterString);
			String type = null;
			while (st.hasMoreTokens()) {
				final String token = st.nextToken();
				if (token.startsWith("{") && token.endsWith("}")) {
					type = token.substring(1, token.length() - 1);
				} else {
					final IParameter parameter;
					boolean optional = false;
					if (token.startsWith("[") && token.endsWith("]")) {
						optional = true;
						String parameterName = token.substring(1,
								token.length() - 1);
						int defaultValueSeperatorIndex = parameterName
								.indexOf('=');
						if (defaultValueSeperatorIndex != -1) {
							parameterName = parameterName.substring(0,
									defaultValueSeperatorIndex);
						}
						parameter = method.getParameter(parameterName);
					} else {
						String parameterName = token;
						int propertiesObjectIndex = parameterName.indexOf('.');
						Type propertiesType = null;
						if (propertiesObjectIndex != -1) {
							String propertyName = parameterName
									.substring(propertiesObjectIndex + 1);
							parameterName = parameterName.substring(0,
									propertiesObjectIndex);

							propertiesType = objectPropertiesTypes
									.get(parameterName);
							if (propertiesType == null) {
								propertiesType = TypeInfoModelFactory.eINSTANCE
										.createType();
								objectPropertiesTypes.put(parameterName,
										propertiesType);
							}
							Property property = TypeInfoModelFactory.eINSTANCE
									.createProperty();
							property.setName(propertyName);
							propertiesType.getMembers().add(property);
						}
						parameter = method.getParameter(parameterName);

						if (parameter != null && propertiesType != null)
							parameter.setPropertiesType(propertiesType);
					}
					if (parameter != null) {
						if (type != null && parameter.getType() == null)
							parameter.setType(type);
						if (!optional && st.hasMoreTokens()
								&& st.nextToken().equals("optional"))
							optional = true;
						parameter.setOptional(optional);
					}
					break;
				}
			}
			index = comment.indexOf(PARAM_TAG, endLineIndex);
		}
	}

	private static final String TYPE_TAG = "@type"; //$NON-NLS-1$
	private static final String RETURNS_TAG = "@returns";

	/**
	 * @see St
	 * @param context
	 * @param member
	 * @param comment
	 */
	private void parseType(IElement member, String comment) {
		int index = comment.indexOf(TYPE_TAG);
		if (index != -1) {
			int endLineIndex = comment.indexOf("\n", index);
			if (endLineIndex == -1) {
				endLineIndex = comment.length();
			}
			StringTokenizer st = new StringTokenizer(comment.substring(index
					+ TYPE_TAG.length(), endLineIndex));
			if (st.hasMoreTokens()) {
				final String typeToken = st.nextToken();
				member.setType(typeToken);
			}
		} else {
			index = comment.indexOf(RETURNS_TAG);
			if (index != -1) {
				int begingBrace = comment.indexOf('{',
						index + RETURNS_TAG.length());
				if (begingBrace != -1) {
					int endBrace = comment.indexOf('}', begingBrace);
					if (endBrace != -1) {
						member.setType(comment.substring(begingBrace + 1,
								endBrace).trim());
					}
				}
			}

		}
	}
}
