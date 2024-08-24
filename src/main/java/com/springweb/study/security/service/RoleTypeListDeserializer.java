package com.springweb.study.security.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.springweb.study.common.RoleType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RoleTypeListDeserializer extends JsonDeserializer<List<RoleType>> {

	@Override
	public List<RoleType> deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String text = p.getText();
		return Arrays.asList(RoleType.valueOf(text));
	}
}