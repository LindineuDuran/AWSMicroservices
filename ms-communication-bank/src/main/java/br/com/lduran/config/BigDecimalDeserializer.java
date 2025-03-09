package br.com.lduran.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

public class BigDecimalDeserializer extends JsonDeserializer<BigDecimal>
{
	@Override
	public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
	{
		String value = p.getText();
		if (value != null && value.startsWith("$"))
		{
			value = value.replace("$", "").trim();
		}
		return new BigDecimal(value);
	}
}