package com.jy.eleaitender.file.engine;

import com.deepoove.poi.data.TextRenderData;
import com.deepoove.poi.policy.TextRenderPolicy;
import com.deepoove.poi.render.RenderContext;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * 文本占位符渲染后仅增加黄色标记，不修改模板原有字体、字号等格式。
 */
public class HighlightTextRenderPolicy extends TextRenderPolicy {

    @Override
    public void doRender(RenderContext<TextRenderData> context) throws Exception {
        super.doRender(context);
        XWPFRun run = context.getRun();
        if (run != null) {
            run.setTextHighlightColor("yellow");
        }
    }
}
