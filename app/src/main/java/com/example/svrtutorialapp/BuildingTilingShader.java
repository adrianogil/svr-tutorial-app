package com.example.svrtutorialapp;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRShaderTemplate;
import com.samsungxr.utility.TextFile;

public class BuildingTilingShader extends SXRShaderTemplate {

    public static final String TEXTURE_KEY = "u_texture";
    public static final String SCALE_KEY = "scale";

    public BuildingTilingShader(SXRContext context) {
        super( "float3 scale","sampler2D u_texture", "float4 a_position, float2 a_texcoord", GLSLESVersion.VULKAN);

        final String fragTemplate = TextFile.readTextFile(context.getContext(), R.raw.building_tiling_frag);
        final String vtxTemplate = TextFile.readTextFile(context.getContext(), R.raw.building_tiling_vert);

        setSegment("VertexTemplate", vtxTemplate);
        setSegment("FragmentTemplate", fragTemplate);
    }
}