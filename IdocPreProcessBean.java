package com.saki.idocpreprocess;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.cci.ConnectionFactory;

import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.conn.idoc.IDocException;
import com.sap.conn.idoc.IDocRepository;
import com.sap.conn.idoc.IDocSegmentMetaData;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.exception.InvalidParamException;
import com.sap.engine.interfaces.messaging.api.exception.MessagingException;
import com.sap.mw.jco.jra.idoc.JRAIDoc;
import com.sap.mw.jco.jra.idoc.JRAIDocFactory;
import com.sap.tc.logging.Location;

/**
 * @author AUSinghVi2
 * This adapter module will convert the flat files to be converted into a format
 * that can be processed by the standard module SAP_XI_IDOC/IDOCFlatToXmlConvertor
 * This is required as the files used in saki are not in the format expected by 
 * the standard bean
 *
 */
public class IdocPreProcessBean implements SessionBean, Module {

	private SessionContext myContext;
	/**
	 * 
	 */

	public static final String NEW_LINE = System.getProperty("line.separator");
	public static final int dataRecordLength = 1062;
	private static final long serialVersionUID = -7818830699858705827L;


	/**
	 * 
	 */
	AuditAccess audit = null;
	MessageKey key = null;

	@Override
	public void ejbActivate() throws EJBException, RemoteException {
		// TODO Auto-generated method stub
	}

	@Override
	public void ejbPassivate() throws EJBException, RemoteException {

	}

	@Override
	public void ejbRemove() throws EJBException, RemoteException {

	}

	/**
	 * 
	 */

	@Override
	public ModuleData process(ModuleContext moduleContext,
			ModuleData inputModuleData) throws ModuleException {
		Location location = null;
		{
			String SIGNATURE = "process(ModuleContext moduleContext, ModuleData inputModuleData)";

			try {
				location = Location.getLocation(IdocPreProcessBean.class
						.getName());
				

				
			} catch (Exception t) {
				t.printStackTrace();
				ModuleException me = new ModuleException(
						"Unable to create trace location", t);
				throw me;
			}
			
			location.entering(SIGNATURE, new Object[] { moduleContext,
					inputModuleData });
			location.debugT(SIGNATURE, "Inside method process");

			Object obj = null;
			Message msg = null;
			String outputData = "";
			List<String> fileDataFinalMultiLine = new ArrayList<String>();
			List<SegDataInfo> segMetaDataList = new ArrayList<SegDataInfo>();
			Boolean checkMetadata;

			obj = inputModuleData.getPrincipalData();
			msg = (Message) obj;			

			if (msg.getMessageDirection().equals(MessageDirection.OUTBOUND)) {
				key = new MessageKey(msg.getMessageId(),
						MessageDirection.OUTBOUND);
			} else {
				key = new MessageKey(msg.getMessageId(),
						MessageDirection.INBOUND);
			}

			try {
				audit = PublicAPIAccessFactory.getPublicAPIAccess()
						.getAuditAccess();
				addLog(key, AuditLogStatus.SUCCESS,
						" IdocPreProcessBean: Module called");
				

			} catch (MessagingException e) {
				addLog(key, AuditLogStatus.SUCCESS,
						" IdocPreProcessBean: Module called");
			}


			// Get data as input stream
			XMLPayload xmlpayload = msg.getDocument();
			InputStream in = msg.getMainPayload().getInputStream();

			// Read input data as list of Strings
			fileDataFinalMultiLine = extractMultiLineList(in, location);

			// Get idoc control record
			IdocControlData idocControl = retrieveControlData(fileDataFinalMultiLine);

			idocControl.sourceJRA = moduleContext.getContextData("SourceJRA");
			idocControl.targetDestination = moduleContext
					.getContextData("TargetDestination");

			if (idocControl.sourceJRA != null && idocControl.sourceJRA != ""
					&& idocControl.targetDestination != null
					&& idocControl.targetDestination != "") {
				checkMetadata = true;
			}

			if (checkMetadata = true) {
				location.debugT(SIGNATURE, "Inside method process ");
				segMetaDataList = retrieveMetadata(idocControl,
						segMetaDataList, key, location);
			}

			// Replace seg definition to segment type
			outputData = replaceSegmentData(fileDataFinalMultiLine,
					segMetaDataList, idocControl, outputData, key,
					checkMetadata);

			byte[] outputBytes = outputData.getBytes();

			if (outputBytes != null) {
				try {
					addLog(key, AuditLogStatus.SUCCESS,
							" IdocPreProcessBean: Data Updated ");

					xmlpayload.setContent(outputBytes);
					inputModuleData.setPrincipalData(msg);
				} catch (InvalidParamException e1) {

					addLog(key, AuditLogStatus.SUCCESS, " Error Occurred"
							+ e1.getMessage());

					locationCatching(SIGNATURE, e1, location);
					ModuleException me = new ModuleException(e1);
					location.throwing(SIGNATURE, me);
					throw me;

				}
			}

			addLog(key, AuditLogStatus.SUCCESS,
					"IdocPreProcessBean: Exiting Module ");

			return inputModuleData;

		}

	}

	/** Replace segment data. This method will process all lines
	 * and convert the data
	 * 
	 * @param _fileDataFinalMultiLine
	 * @param _segMetaDataList
	 * @param _idocControl
	 * @param outputData
	 * @param key
	 * @param checkMetadata
	 * @return
	 */
	private String replaceSegmentData(List<String> _fileDataFinalMultiLine,
			List<SegDataInfo> _segMetaDataList, IdocControlData _idocControl,
			String outputData, MessageKey key, Boolean checkMetadata) {

		StringBuffer strBuffer = new StringBuffer();

		for (String tempLineString : _fileDataFinalMultiLine) {

			String controlRecordString = "";

			if (tempLineString.substring(0, 8).equals("EDI_DC40")) {

				if (tempLineString.length() > 394) {
					controlRecordString = tempLineString.substring(0, 377)
							+ "                "
							+ tempLineString.substring(394); // Blank out date and time fields
				} else {
					controlRecordString = tempLineString;
				}

				if (strBuffer.length() == 0) {
					strBuffer.append(controlRecordString);
				} else {
					strBuffer.append(NEW_LINE);
					strBuffer.append(controlRecordString);
				}
			} else {

				String segName = tempLineString.substring(0, 29);
				String segmentData = tempLineString.substring(30);
				if (!_idocControl.fileHasSegDef) {
					strBuffer.append(NEW_LINE);
					strBuffer.append(tempLineString);
				} else {
					strBuffer = replaceSegmentDataForASegmentDef(segmentData,
							segName, _segMetaDataList, _idocControl, strBuffer,
							key, checkMetadata);

				}

			}

		}

		return strBuffer.toString();

	}

	/**
	 * Retrieve metadata using the basic type and extension
	 * 
	 * @param _idocControl
	 * @param _segMetaDataList
	 * @param key
	 * @return
	 * @throws ModuleException
	 */
	private List<SegDataInfo> retrieveMetadata(IdocControlData _idocControl,
			List<SegDataInfo> _segMetaDataList, MessageKey key,
			Location location) throws ModuleException {

		String SIGNATURE = "retrieveMetadata(IdocControlData _idocControl,List<SegDataInfo> _segMetaDataList, MessageKey key)";

		addLog(key, AuditLogStatus.SUCCESS, "Extracting metadata for "
				+ _idocControl.basicType + " " + _idocControl.extension);

		IDocSegmentMetaData rootMetaData = null;
		SegDataInfo segData = new SegDataInfo();
		try {
			InitialContext initialcontext = new InitialContext();

			ConnectionFactory connectionFactory = (ConnectionFactory) initialcontext
					.lookup((new StringBuilder()).append("deployedAdapters/")
							.append(_idocControl.sourceJRA).append(
									"/shareable/").append(
									_idocControl.sourceJRA).toString());

			JRAIDocFactory idocFactory = JRAIDoc.getIDocFactory();
			IDocRepository idocRepository = idocFactory
					.getIDocRepository(connectionFactory);
			
			
			rootMetaData = idocRepository.getRootSegmentMetaData(
					_idocControl.basicType, _idocControl.extension, "", "");
			if (rootMetaData != null) {
				segData.getMetaData(rootMetaData, _segMetaDataList);
				return _segMetaDataList;
			}

		} catch (NamingException e) {
			locationCatching(SIGNATURE, e, location);
			ModuleException me = new ModuleException(e);
			location.throwing(SIGNATURE, me);
			throw me;
	} catch (IDocException e) {
			locationCatching(SIGNATURE, e, location);
			ModuleException me = new ModuleException(e);
			location.throwing(SIGNATURE, me);
			throw me;
			
		}

		return _segMetaDataList;
	}

	/*
	 * Extract control record from file data which is supplied as a string
	 * arraylist
	 * 
	 * @param _fileDataFinalMultiLine
	 * 
	 * @return
	 */
	private IdocControlData retrieveControlData(
			List<String> _fileDataFinalMultiLine) {

		IdocControlData idocData = new IdocControlData();

		int i = 0;
		for (String tempLineString : _fileDataFinalMultiLine) {
			if (tempLineString.substring(0, 8).equals("EDI_DC40")) {
				String EDI_DC40 = tempLineString;
				idocData.sapRelease = EDI_DC40.substring(29, 32).trim();
				idocData.basicType = EDI_DC40.substring(39, 68).trim();
				idocData.extension = EDI_DC40.substring(69, 98).trim();
			}

			if (i++ > 0) {
				idocData.fileHasSegDef = IsSegmentDefinition(tempLineString
						.substring(0, 30).trim());
				break;
			}
		}
		return idocData;
	}

	/**
	 * Right padding of a string
	 * 
	 * @param s
	 * @param n
	 * @return
	 */
	public static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}

	/**
	 * Add Log for tracing When run in NWDS, it gets printed
	 * 
	 * @param msgKey
	 * @param status
	 * @param message
	 */
	private void addLog(MessageKey msgKey, AuditLogStatus status, String message) {
		if (audit != null) {
			audit.addAuditLogEntry(msgKey, status, message);
		} else {
			System.out.println("Audit Log: " + message);
		}
	}

	/**
	 * Use the file data to extract file data as a String list.
	 * 
	 * @param _inputStream
	 * @return
	 * @throws ModuleException
	 */
	private List<String> extractMultiLineList(InputStream _inputStream,
			Location location) throws ModuleException {

		/**
		 * addLog(key, AuditLogStatus.SUCCESS,
		 * " IdocPreProcessBean: extractMultiLineList called ");
		 */

		String SIGNATURE = "extractMultiLineList(InputStream _inputStream)";
		List<String> fileDataAlreadyMultiLine = new ArrayList<String>();
		List<String> fileDataFinalMultiLine = new ArrayList<String>();
		List<String> tempMultiLine = new ArrayList<String>();
		String line = "";
		int numberOfLines = 0;

		byte[] search;
		try {
			search = "\r".getBytes("UTF-8");
			byte[] replacement = "\r\n".getBytes("UTF-8");

			InputStream ris = new ReplacingInputStream(_inputStream, search,
					replacement);

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(ris));

				while ((line = reader.readLine()) != null) {
					numberOfLines++;
					tempMultiLine.add(line);
				}
				reader.close();
			} catch (IOException e1) {

				locationCatching(SIGNATURE, e1, location);
				ModuleException me = new ModuleException(e1);
				location.throwing(SIGNATURE, me);
				throw me;

			}

			if (tempMultiLine.size() == 1) { // File is in ALE format, convert
				// to multi line format
				fileDataAlreadyMultiLine = convertALEFileToMultiLineFormat(tempMultiLine);
			} else {
				fileDataAlreadyMultiLine = tempMultiLine;
			}

			fileDataFinalMultiLine = removeLeadingControlDataStrings(fileDataAlreadyMultiLine);

		} catch (UnsupportedEncodingException e) {
			
			locationCatching(SIGNATURE, e, location);
			ModuleException me = new ModuleException(e);
			location.throwing(SIGNATURE, me);
			throw me;
			
		}

		return fileDataFinalMultiLine;

	}

	/**
	 * Check if the segment is a segment type or segment definition
	 * 
	 * @param SegmentName
	 * @return
	 */
	private Boolean IsSegmentDefinition(String SegmentName) {

		if (SegmentName.substring(0, 2).equals("E2")
				|| SegmentName.substring(SegmentName.length() - 3).matches(
						"[0-9]+"))

			return true;
		else
			return false;
	}

	/**
	 * Removes leading control and data records and converts the data to a
	 * multiline list
	 * 
	 * @param _fileDataAlreadyMultiLine
	 * @return
	 */
	private List<String> removeLeadingControlDataStrings(
			List<String> _fileDataAlreadyMultiLine) {

		List<String> fileDataFinalMultiLine = new ArrayList<String>();

		for (String str : _fileDataAlreadyMultiLine) {

			if (str.substring(0, 7).equals("CONTROL")
					|| str.substring(0, 4).equals("DATA")) {
				String tempLineString = str.substring(10);

				fileDataFinalMultiLine.add(tempLineString);
			} else {
				fileDataFinalMultiLine.add(str);
			}

		}
		return fileDataFinalMultiLine;
	}

	/**
	 * converts ALE fileformat to multiline file format ALE file format has
	 * control and data record as a continuous line
	 * 
	 * @param _fileDataAlreadyMultiLine
	 * @return
	 */
	private List<String> convertALEFileToMultiLineFormat(
			List<String> _fileDataAlreadyMultiLine) {

		List<String> fileMultiLineList = new ArrayList<String>();

		String lineCopy = "";

		// Extract data as string
		for (String str : _fileDataAlreadyMultiLine) {
			lineCopy = str;
		}
		// Extract control and Data Record
		String controlRecord = lineCopy.substring(0, 523);
		fileMultiLineList.add(controlRecord);

		String dataRecord = "";
		String dataRecordTemp = lineCopy.substring(524, lineCopy.length());

		int dataRead = 0;
		int start = 0;
		int end = dataRecordLength;

		while (dataRead < dataRecordTemp.length()) {
			end = start + dataRecordLength;
			// If reached end, set end to last character
			if (end - dataRecordTemp.length() > 0) {
				end = dataRecordTemp.length() - 1;
			}

			if (dataRecord == "") {
				dataRecord = dataRecordTemp.substring(start, end);
				fileMultiLineList.add(dataRecord.trim());
			} else {
				if (start < end) {
					dataRecord = dataRecordTemp.substring(start, end);
					fileMultiLineList.add(dataRecord);

				}
			}
			start = end + 1;
			dataRead = dataRead + dataRecordLength;
		}
		return fileMultiLineList;
	}

	/*
	 * Replaces degment definition by segment type Needs idoc metadata to be
	 * populated
	 * 
	 * @param _segmentData
	 * 
	 * @param _segName
	 * 
	 * @param _segMetaDataList
	 * 
	 * @param _idocControl
	 * 
	 * @param strBuffer
	 * 
	 * @param key
	 * 
	 * @param checkMetadata
	 * 
	 * @return
	 */
	private StringBuffer replaceSegmentDataForASegmentDef(String _segmentData,
			String _segName, List<SegDataInfo> _segMetaDataList,
			IdocControlData _idocControl, StringBuffer strBuffer,
			MessageKey key, Boolean checkMetadata) {

		int len = _segMetaDataList.size();

		for (int i = 0; i < len; i++) {
			SegDataInfo segMetaDataTemp = _segMetaDataList.get(i);

			if (segMetaDataTemp.getSegDef().trim().toUpperCase().equals(
					_segName.trim().toUpperCase())) {
				String tempSegType = segMetaDataTemp.getSegType();
				String lineToReplace = padRight(tempSegType, 30) + _segmentData;
				strBuffer.append(NEW_LINE);
				strBuffer.append(lineToReplace);
				break;
			}
		}
		return strBuffer;
	}

	
	
	
	/** Method to display the location of the error
	 * @param signature
	 * @param t
	 * @param location
	 */
	private void locationCatching(String signature, Throwable t,
			Location location) {
		if ((location != null) && (location.beLogged(400))) {
			ByteArrayOutputStream oStream = new ByteArrayOutputStream(1024);
			PrintStream pStream = new PrintStream(oStream);
			t.printStackTrace(pStream);
			pStream.close();
			String stackTrace = oStream.toString();
			location.warningT(signature, "Catching {0}",
					new Object[] { stackTrace });
		}
	}

	@Override
	public void setSessionContext(SessionContext arg0) throws EJBException,
			RemoteException {

	}

}
