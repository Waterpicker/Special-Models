package net.ludocrypt.specialmodels.impl.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class SpecialVertexFormats {

	public static final VertexFormatElement STATE_ELEMENT = new VertexFormatElement(0, VertexFormatElement.Type.BYTE,
		VertexFormatElement.Usage.GENERIC, 4);
	public static final VertexFormat POSITION_COLOR_TEXTURE_LIGHT_NORMAL_STATE = new VertexFormat(ImmutableMap
		.of("Position", DefaultVertexFormat.ELEMENT_POSITION, "Color", DefaultVertexFormat.ELEMENT_COLOR, "UV0",
			DefaultVertexFormat.ELEMENT_UV0, "UV2", DefaultVertexFormat.ELEMENT_UV2, "Normal", DefaultVertexFormat.ELEMENT_NORMAL,
			"State", STATE_ELEMENT, "Padding", DefaultVertexFormat.ELEMENT_PADDING));

}
