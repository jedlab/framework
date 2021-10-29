package com.jedlab.framework.spring.rest;

import org.omidbiz.core.axon.internal.Axon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jedlab.framework.json.JacksonView;

@Axon
//@JsonIgnoreProperties(value = { "typeName" })
//@JsonFilter("JsonViewFilter")
@JacksonView
public class ResultObjectMessage<T> extends ResponseMessage {

	private T result;

	public ResultObjectMessage(String message, int code, T result) {
		super(message, code);
		this.result = result;
	}

	public T getResult() {
		return result;
	}

}
