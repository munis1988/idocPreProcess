package com.saki.idocpreprocess;

public class IdocControlData {
	
	public String getSegVersion() {
		return segVersion;
	}
	public void setSegVersion(String segVersion) {
		this.segVersion = segVersion;
	}
	String messageType;
	String basicType;
	String extension;
	String segVersion;
	String sapRelease;
	String sourceJRA;
	String targetDestination;
	Boolean fileHasSegDef;
	
	
	public Boolean getFileHasSegDef() {
		return fileHasSegDef;
	}
	public void setFileHasSegDef(Boolean fileHasSegDef) {
		this.fileHasSegDef = fileHasSegDef;
	}
	public String getTargetDestination() {
		return targetDestination;
	}
	public void setTargetDestination(String targetDestination) {
		this.targetDestination = targetDestination;
	}
	public String getSapRelease() {
		return sapRelease;
	}
	public void setSapRelease(String sapRelease) {
		this.sapRelease = sapRelease;
	}
	public String getSourceJRA() {
		return sourceJRA;
	}
	public void setSourceJRA(String sourceJRA) {
		this.sourceJRA = sourceJRA;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getBasicType() {
		return basicType;
	}
	public void setBasicType(String basicType) {
		this.basicType = basicType;
	}
	public String getExtension() {
		return extension;
	}
	public void setExtension(String extension) {
		this.extension = extension;
	}

}
