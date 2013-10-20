/* * Copyright (C) 2005-2010 Alfresco Software Limited. * * This file is part of Alfresco * * Alfresco is free software: you can redistribute it and/or modify * it under the terms of the GNU Lesser General Public License as published by * the Free Software Foundation, either version 3 of the License, or * (at your option) any later version. * * Alfresco is distributed in the hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the * GNU Lesser General Public License for more details. * * You should have received a copy of the GNU Lesser General Public License * along with Alfresco. If not, see <http://www.gnu.org/licenses/>. */package de.fme.alfresco.repo.web.scripts.datalist;import java.io.BufferedReader;import java.io.IOException;import java.io.InputStreamReader;import java.io.Serializable;import java.util.ArrayList;import java.util.Arrays;import java.util.Date;import java.util.HashMap;import java.util.HashSet;import java.util.List;import java.util.Map;import java.util.Set;import java.util.StringTokenizer;import org.alfresco.model.ApplicationModel;import org.alfresco.model.ContentModel;import org.alfresco.model.ForumModel;import org.alfresco.repo.content.MimetypeMap;import org.alfresco.service.cmr.dictionary.PropertyDefinition;import org.alfresco.service.cmr.dictionary.TypeDefinition;import org.alfresco.service.cmr.repository.AssociationRef;import org.alfresco.service.cmr.repository.ChildAssociationRef;import org.alfresco.service.cmr.repository.ContentData;import org.alfresco.service.cmr.repository.ContentIOException;import org.alfresco.service.cmr.repository.ContentReader;import org.alfresco.service.cmr.repository.ContentService;import org.alfresco.service.cmr.repository.ContentWriter;import org.alfresco.service.cmr.repository.NodeRef;import org.alfresco.service.cmr.repository.NodeService;import org.alfresco.service.cmr.security.PersonService;import org.alfresco.service.cmr.site.SiteInfo;import org.alfresco.service.cmr.site.SiteService;import org.alfresco.service.namespace.InvalidQNameException;import org.alfresco.service.namespace.NamespaceService;import org.alfresco.service.namespace.QName;import org.alfresco.util.Pair;import org.apache.commons.csv.CSVPrinter;import org.apache.commons.lang.time.DateFormatUtils;import org.apache.commons.logging.Log;import org.apache.commons.logging.LogFactory;import org.apache.poi.ss.usermodel.Cell;import org.apache.poi.ss.usermodel.CellStyle;import org.apache.poi.ss.usermodel.CreationHelper;import org.apache.poi.ss.usermodel.DataFormat;import org.apache.poi.ss.usermodel.Font;import org.apache.poi.ss.usermodel.IndexedColors;import org.apache.poi.ss.usermodel.Row;import org.apache.poi.ss.usermodel.Sheet;import org.apache.poi.ss.usermodel.Workbook;import org.apache.tika.parser.ParsingReader;import org.springframework.beans.factory.InitializingBean;import org.springframework.extensions.webscripts.Status;import org.springframework.extensions.webscripts.WebScriptException;import org.springframework.extensions.webscripts.WebScriptRequest;import de.fme.alfresco.repo.web.scripts.DeclarativeSpreadsheetWebScript;/** * Data List Download *  * Exports the contents of a Data List as an Excel file *  * @author Nick Burch, Jan Pfitzner */public class DataListDownloadWebScript extends DeclarativeSpreadsheetWebScript		implements InitializingBean {	// Logger	private static final Log logger = LogFactory			.getLog(DataListDownloadWebScript.class);	private static final String DL_NAMESPACE = "http://www.alfresco.org/model/datalist/1.0";	private static final String SMOT_NAMESPACE = "http://www.acando.com/model/1.0";	static final QName TYPE_PRODUKTION = QName.createQName(SMOT_NAMESPACE,			"produktion");	static final QName TYPE_PERSONRESURS = QName.createQName(SMOT_NAMESPACE,			"person");	static final QName PROP_FORNAMN = QName.createQName(SMOT_NAMESPACE,			"fornamn");	static final QName PROP_EFTERNAMN = QName.createQName(SMOT_NAMESPACE,			"efternamn");	private static final QName DATA_LIST_ITEM_TYPE = QName.createQName(			DL_NAMESPACE, "dataListItemType");	private NodeService nodeService;	private SiteService siteService;	private ContentService contentService;	private PersonService personService;	private NamespaceService namespaceService;	private Map<QName, List<QName>> modelOrder;	private Map<String, String> rawModelOrder;	public DataListDownloadWebScript() {		this.filenameBase = "DataListExport";	}	/**	 * @param nodeService	 */	public void setNodeService(NodeService nodeService) {		this.nodeService = nodeService;	}	/**	 * @param nodeService	 */	public void setSiteService(SiteService siteService) {		this.siteService = siteService;	}	/**	 * @param namespaceService	 */	public void setNamespaceService(NamespaceService namespaceService) {		this.namespaceService = namespaceService;	}	/**	 * @param contentService	 *            the contentService to set	 */	public void setContentService(ContentService contentService) {		this.contentService = contentService;	}	/**	 * @param personService	 *            the personService to set	 */	public void setPersonService(PersonService personService) {		this.personService = personService;	}	public void setModelOrder(Map<String, String> rawModelOrder) {		this.rawModelOrder = rawModelOrder;	}	public void afterPropertiesSet() throws Exception {		modelOrder = new HashMap<QName, List<QName>>();		for (String key : rawModelOrder.keySet()) {			QName model;			List<QName> order = new ArrayList<QName>();			try {				model = QName.createQName(key, namespaceService);			} catch (InvalidQNameException e) {				logger.warn("Skipping invalid model type " + key);				continue;			}			StringTokenizer st = new StringTokenizer(rawModelOrder.get(key),					",");			while (st.hasMoreTokens()) {				order.add(QName.createQName(st.nextToken(), namespaceService));			}			modelOrder.put(model, order);		}	}	/**	 * Identify the datalist	 */	@Override	protected Object identifyResource(String format, WebScriptRequest req) {		// Try to find the datalist they requested		NodeRef list;		Map<String, String> args = req.getServiceMatch().getTemplateVars();		if (args.get("store_type") != null) {			list = new NodeRef(args.get("store_type"), args.get("store_id"),					args.get("id"));		} else {			// Get the site			SiteInfo site = siteService.getSite(args.get("site"));			if (site == null) {				throw new WebScriptException(Status.STATUS_NOT_FOUND,						"Site not found with supplied name");			}			// Now find the data list container with in			NodeRef container = nodeService.getChildByName(site.getNodeRef(),					ContentModel.ASSOC_CONTAINS, args.get("container"));			if (container == null) {				throw new WebScriptException(Status.STATUS_NOT_FOUND,						"Container not found within site");			}			// Now get the data list itself			list = nodeService.getChildByName(container,					ContentModel.ASSOC_CONTAINS, args.get("list"));		}		if (list == null || !nodeService.exists(list)) {			throw new WebScriptException(Status.STATUS_NOT_FOUND,					"The Data List could not be found");		}		return list;	}	/**	 * We don't have a HTML version	 */	@Override	protected boolean allowHtmlFallback() {		return false;	}	/**	 * Fetch the properties, in the requested order, from the data list	 * definition	 */	@Override	protected List<Pair<QName, Boolean>> buildPropertiesForHeader(			Object resource, String format, WebScriptRequest req) {		NodeRef list = (NodeRef) resource;		QName type = buildType(list);		// Has the user given us rules for what to do		// with this type?		List<QName> props;		if (modelOrder.containsKey(type)) {			props = modelOrder.get(type);		} else {			// We'll have to try to guess it for them			// For now, just use DataList properties for the type			TypeDefinition typeDef = dictionaryService.getType(type);			Map<QName, PropertyDefinition> allProps = typeDef.getProperties();			props = new ArrayList<QName>();			for (QName prop : allProps.keySet()) {				// if(DL_NAMESPACE.equals(prop.getNamespaceURI()))				// {				props.add(prop);				// }			}		}		// Everything is required		List<Pair<QName, Boolean>> properties = new ArrayList<Pair<QName, Boolean>>();		for (QName qname : props) {			properties.add(new Pair<QName, Boolean>(qname, true));		}		return properties;	}	private QName buildType(NodeRef list) {		String typeS = (String) nodeService.getProperty(list,				DATA_LIST_ITEM_TYPE);		return QName.createQName(typeS, namespaceService);	}	private List<NodeRef> getItems(NodeRef list) {		Set<QName> typeSet = new HashSet<QName>(				Arrays.asList(new QName[] { buildType(list) }));		List<NodeRef> items = new ArrayList<NodeRef>();		for (ChildAssociationRef ca : nodeService.getChildAssocs(list, typeSet)) {			items.add(ca.getChildRef());		}		return items;	}	@Override	protected void populateBody(Object resource, CSVPrinter csv,			List<QName> properties) throws IOException {		throw new WebScriptException(Status.STATUS_BAD_REQUEST,				"CSV not currently supported");	}	@SuppressWarnings("deprecation")	@Override	protected void populateBody(Object resource, Workbook workbook,			Sheet sheet, List<QName> properties) throws IOException {		NodeRef list = (NodeRef) resource;		List<NodeRef> items = getItems(list);		// Our various formats		DataFormat formatter = workbook.createDataFormat();		CreationHelper createHelper = workbook.getCreationHelper();		CellStyle styleInt = workbook.createCellStyle();		styleInt.setDataFormat(formatter.getFormat("0"));		CellStyle styleDate = workbook.createCellStyle();		styleDate.setDataFormat(formatter.getFormat("yyyy-mm-dd"));		CellStyle styleDouble = workbook.createCellStyle();		styleDouble.setDataFormat(formatter.getFormat("General"));		CellStyle styleNewLines = workbook.createCellStyle();		styleNewLines.setWrapText(true);		CellStyle hlink_style = workbook.createCellStyle();		Font hlink_font = workbook.createFont();		hlink_font.setUnderline(Font.U_SINGLE);		hlink_font.setColor(IndexedColors.BLUE.getIndex());		hlink_style.setFont(hlink_font);		// Export the items		int rowNum = 1, colNum = 0;		for (NodeRef item : items) {			Row r = sheet.createRow(rowNum);			colNum = 0;			for (QName prop : properties) {				Cell c = r.createCell(colNum);				Serializable val = nodeService.getProperty(item, prop);				if (val == null) {					// Is it an association, or just missing?					List<AssociationRef> assocs = nodeService.getTargetAssocs(							item, prop);					Set<QName> qnames = new HashSet<QName>(1, 1.0f);					qnames.add(prop);					List<ChildAssociationRef> childAssocs = nodeService							.getChildAssocs(item, qnames);					if (assocs.size() > 0) {						StringBuffer text = new StringBuffer();						int lines = 1;						for (AssociationRef ref : assocs) {							NodeRef child = ref.getTargetRef();							QName type = nodeService.getType(child);							if (ContentModel.TYPE_PERSON.equals(type)) {								if (text.length() > 0) {									text.append('\n');									lines++;								}								text.append(nodeService.getProperty(child,										ContentModel.PROP_FIRSTNAME));								text.append(" ");								text.append(nodeService.getProperty(child,										ContentModel.PROP_LASTNAME));							} else if (ContentModel.TYPE_CONTENT.equals(type)) {								// TODO Link to the content								if (text.length() > 0) {									text.append('\n');									lines++;								}								text.append(nodeService.getProperty(child,										ContentModel.PROP_NAME));								text.append(" (");								text.append(nodeService.getProperty(child,										ContentModel.PROP_TITLE));								text.append(") ");								/*								 * MessageFormat.format(CONTENT_DOWNLOAD_PROP_URL								 * , new Object[] {								 * child.getStoreRef().getProtocol(),								 * child.getStoreRef().getIdentifier(),								 * child.getId(),								 * URLEncoder.encode((String)nodeService								 * .getProperty(child,								 * ContentModel.PROP_TITLE)),								 * URLEncoder.encode(ContentModel								 * .PROP_CONTENT.toString()) });								 */								/*								 * currently only one link per cell possible								 * Hyperlink link =								 * createHelper.createHyperlink(								 * Hyperlink.LINK_URL);								 * link.setAddress("http://poi.apache.org/");								 * c.setHyperlink(link);								 * c.setCellStyle(hlink_style);								 */							} else if (ApplicationModel.TYPE_FILELINK									.equals(type)) {								NodeRef linkRef = (NodeRef) nodeService										.getProperty(												child,												ContentModel.PROP_LINK_DESTINATION);								if (linkRef != null) {									if (text.length() > 0) {										text.append('\n');										lines++;									}									text.append("link to: ");									try {										text.append(nodeService												.getProperty(linkRef,														ContentModel.PROP_NAME));										text.append(" (");										text.append(nodeService.getProperty(												linkRef,												ContentModel.PROP_TITLE));										text.append(") ");									} catch (Exception e) {										text.append(nodeService.getProperty(												child, ContentModel.PROP_NAME));										text.append(" (");										text.append(nodeService.getProperty(												child, ContentModel.PROP_TITLE));										text.append(") ");									}								}							} else if (TYPE_PERSONRESURS.equals(type)) {								if (text.length() > 0) {									text.append('\n');									lines++;								}								text.append(nodeService.getProperty(child,										PROP_FORNAMN));								text.append(" ");								text.append(nodeService.getProperty(child,										PROP_EFTERNAMN));							} else if (TYPE_PRODUKTION.equals(type)) {								if (text.length() > 0) {									text.append('\n');									lines++;								}								text.append(nodeService.getProperty(child,										ContentModel.PROP_TITLE));							} else							{								System.err.println("TODO: handle " + type										+ " for " + child);							}						}						String v = text.toString();						c.setCellValue(v);						if (lines > 1) {							c.setCellStyle(styleNewLines);							r.setHeightInPoints(lines									* sheet.getDefaultRowHeightInPoints());						}					} else if (childAssocs.size() > 0) {						StringBuffer text = new StringBuffer();						for (ChildAssociationRef childAssociationRef : childAssocs) {							NodeRef child = childAssociationRef.getChildRef();							QName type = nodeService.getType(child);							if (type.equals(ForumModel.TYPE_FORUM)) {								List<ChildAssociationRef> topics = nodeService										.getChildAssocs(child);								if (topics.size() > 0) {									ChildAssociationRef topicRef = topics											.get(0);									List<ChildAssociationRef> comments = nodeService											.getChildAssocs(topicRef													.getChildRef());									for (ChildAssociationRef commentChildRef : comments) {										NodeRef commentRef = commentChildRef												.getChildRef();										ContentData data = (ContentData) nodeService												.getProperty(														commentRef,														ContentModel.PROP_CONTENT);										TemplateContentData contentData = new TemplateContentData(												data, ContentModel.PROP_CONTENT);										String commentString = "";										try {											commentString = contentData													.getContentAsText(															commentRef, -1);										} catch (Exception e) {											logger.warn(													"failed to extract content for nodeRef "															+ commentRef, e);										}										String creator = (String) nodeService												.getProperty(														commentRef,														ContentModel.PROP_CREATOR);										NodeRef person = personService												.getPerson(creator, false);										if (person != null) {											creator = nodeService													.getProperty(															person,															ContentModel.PROP_FIRSTNAME)													+ " "													+ nodeService															.getProperty(																	person,																	ContentModel.PROP_LASTNAME);										}										Date created = (Date) nodeService												.getProperty(														commentRef,														ContentModel.PROP_CREATED);										text.append(creator)												.append(" (")												.append(DateFormatUtils.format(														created, "yyyy-MM-dd"))												.append("):\n ");										text.append(commentString).append("\n");									}								}							}						}						String v = text.toString();						c.setCellValue(v);						c.setCellStyle(styleNewLines);					} else {						// This property isn't set						c.setCellType(Cell.CELL_TYPE_BLANK);					}				} else {					// Regular property, set					if (val instanceof String) {						c.setCellValue((String) val);						c.setCellStyle(styleNewLines);					} else if (val instanceof Date) {						c.setCellValue((Date) val);						c.setCellStyle(styleDate);					} else if (val instanceof Integer || val instanceof Long) {						double v = 0.0;						if (val instanceof Long)							v = (double) (Long) val;						if (val instanceof Integer)							v = (double) (Integer) val;						c.setCellValue(v);						c.setCellStyle(styleInt);					} else if (val instanceof Float || val instanceof Double) {						double v = 0.0;						if (val instanceof Float)							v = (double) (Float) val;						if (val instanceof Double)							v = (double) (Double) val;						c.setCellValue(v);						c.setCellStyle(styleDouble);					} else {						// TODO						System.err.println("TODO: handle "								+ val.getClass().getName() + " - " + val);					}				}				colNum++;			}			rowNum++;		}		// Sensible column widths please!		colNum = 0;		for (QName prop : properties) {			try {				sheet.autoSizeColumn(colNum);			} catch (IllegalArgumentException e) {				sheet.setColumnWidth(colNum, 40 * 256);			}			colNum++;		}	}	/**	 * Inner class wrapping and providing access to a ContentData property.	 * Slightly modified copy of	 * {@link org.alfresco.repo.template.BaseContentNode.TemplateContentData}	 */	private class TemplateContentData {		private ContentData contentData;		private QName property;		/**		 * Constructor		 * 		 * @param contentData		 *            The ContentData object this object wraps		 * @param property		 *            The property the ContentData is attached too		 */		public TemplateContentData(ContentData contentData, QName property) {			this.contentData = contentData;			this.property = property;		}		/**		 * @param nodeRef		 * @param length		 * @return the content stream to the specified maximum length in		 *         characters		 */		public String getContentMaxLength(final NodeRef nodeRef, int length) {			ContentReader reader = contentService.getReader(nodeRef, property);			return (reader != null && reader.exists()) ? reader					.getContentString(length) : "";		}		/**		 * @param nodeRef		 * @param length		 *            Length of the character stream to return, or -1 for all		 * @return the binary content stream converted to text using any		 *         available transformer if fails to convert then null will be		 *         returned		 */		public String getContentAsText(final NodeRef nodeRef, int length) {			String result = null;			String mimetype = contentData.getMimetype();			if (MimetypeMap.MIMETYPE_TEXT_PLAIN.equals(mimetype)) {				result = getContentMaxLength(nodeRef, length);			} else {				// try to use Apache Tika Framework				if (!MimetypeMap.MIMETYPE_HTML.equals(mimetype)) {					result = getContentTextViaTika(nodeRef, length);				}				if (result == null || "".equalsIgnoreCase(result.trim())) {					result = getContentTextViaAlfresco(nodeRef, length);				}			}			return result;		}		private String getContentTextViaAlfresco(final NodeRef nodeRef,				int length) {			String result = null;			// get the content reader			ContentReader reader = contentService.getReader(nodeRef, property);			// get the writer and set it up for text convert			ContentWriter writer = contentService.getWriter(null,					ContentModel.PROP_CONTENT, true);			writer.setMimetype("text/plain");			writer.setEncoding(reader.getEncoding());			// try and transform the content			if (contentService.isTransformable(reader, writer)) {				contentService.transform(reader, writer);				ContentReader resultReader = writer.getReader();				if (resultReader != null && reader.exists()) {					if (length != -1) {						result = getContentString(resultReader, length);					} else {						result = resultReader.getContentString();					}				}			}			return result;		}		private String getContentTextViaTika(final NodeRef nodeRef, int length) {			// get the content reader			String result = null;			ContentReader reader = contentService.getReader(nodeRef, property);			ParsingReader parsingReader = null;			try {				parsingReader = new ParsingReader(						reader.getContentInputStream());				result = getContentString(parsingReader, length);			} catch (ContentIOException e) {				logger.error(e);			} catch (IOException e) {				logger.error(e);			} finally {				if (parsingReader != null) {					try {						parsingReader.close();					} catch (IOException e) {						logger.error(e);					}				}			}			return result;		}		private String getContentString(ParsingReader parsingReader, int length)				throws ContentIOException {			if (length < 0 || length > Integer.MAX_VALUE) {				throw new IllegalArgumentException(						"Character count must be positive and within range");			}			BufferedReader reader = null;			try {				reader = new BufferedReader(parsingReader);				StringBuilder stringBuilder = new StringBuilder();				String line = null;				while ((line = reader.readLine()) != null						&& stringBuilder.length() < length) {					stringBuilder.append(line + "\n");				}				return stringBuilder.toString();			} catch (IOException e) {				logger.error(e);				throw new ContentIOException(						"Failed to copy content to string: \n"								+ "   accessor: " + this + "\n" + "   length: "								+ length, e);			} finally {				if (reader != null) {					try {						reader.close();					} catch (Throwable e) {						logger.error(e);					}				}			}		}		private String getContentString(ContentReader contentReader, int length)				throws ContentIOException {			if (length < 0 || length > Integer.MAX_VALUE) {				throw new IllegalArgumentException(						"Character count must be positive and within range");			}			BufferedReader reader = null;			try {				String encoding = contentReader.getEncoding();				// create a reader from the input stream				if (encoding == null) {					reader = new BufferedReader(new InputStreamReader(							contentReader.getContentInputStream()));				} else {					reader = new BufferedReader(new InputStreamReader(							contentReader.getContentInputStream(), encoding));				}				StringBuilder stringBuilder = new StringBuilder();				String line = null;				while ((line = reader.readLine()) != null						&& stringBuilder.length() < length) {					stringBuilder.append(line + "\n");				}				return stringBuilder.toString();			} catch (IOException e) {				throw new ContentIOException(						"Failed to copy content to string: \n"								+ "   accessor: " + this + "\n" + "   length: "								+ length, e);			} finally {				if (reader != null) {					try {						reader.close();					} catch (Throwable e) {						logger.error(e);					}				}			}		}	}}