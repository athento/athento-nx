package org.athento.nuxeo.ui.widget;

import com.sun.faces.facelets.tag.TagAttributesImpl;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractWidgetTypeHandler;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.renderer.NXCheckboxRenderer;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.view.facelets.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkbox widget that generates a checkbox for a boolean value in edit mode,
 * and displays the boolean value in view mode.
 * <p>
 * In view mode, show a check icon from font-awesome in a 'icon' property of the widget.
 */
public class BooleanWithIconWidgetTypeHandler extends AbstractWidgetTypeHandler {
    
    private static final long serialVersionUID = 1L;

    private static final String STYLECLASS_ICON_FORMAT = "fa fa-%s ath--boolean-icon";

    public BooleanWithIconWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget,
                null, helper);
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            TagAttributes attributes = helper.getTagAttributes(widgetId, widget);
            ComponentHandler input = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, leaf,
                    HtmlSelectBooleanCheckbox.COMPONENT_TYPE,
                    NXCheckboxRenderer.RENDERER_TYPE);
            String msgId = helper.generateMessageId(ctx, widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(
                    widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            FaceletHandler h = new CompositeFaceletHandler(handlers);
            h.apply(ctx, parent);
        } else {
            TagAttributes attributes = getViewTagAttributes(helper,
                    widgetId, widget, !BuiltinWidgetModes.isLikePlainMode(mode));
            // default on text for other modes
            ComponentHandler output = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, leaf,
                    HtmlOutputText.COMPONENT_TYPE, null);
            if (BuiltinWidgetModes.PDF.equals(mode)) {
                // add a surrounding p:html tag handler
                FaceletHandler handler = helper.getHtmlComponentHandler(widgetTagConfigId,
                        new TagAttributesImpl(new TagAttribute[0]), output,
                        UIHtmlText.class.getName(), null);
                FaceletHandler[] handlers = { handler };
                FaceletHandler h = new CompositeFaceletHandler(handlers);
                h.apply(ctx, parent);
            } else {
                FaceletHandler[] handlers = { output };
                FaceletHandler h = new CompositeFaceletHandler(handlers);
                h.apply(ctx, parent);
            }
        }
    }

    /**
     * Return tag attributes after having replaced the usual value expression
     * for the 'value' field by a specific expression to display font-awesome icon to checked or unchecked.
     */
    protected TagAttributes getViewTagAttributes(FaceletHandlerHelper helper, String id, Widget widget, boolean addId) {
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        FieldDefinition[] fields = widget.getFieldDefinitions();
        if (fields != null && fields.length > 0) {
            FieldDefinition field = fields[0];
            String bareExpression = ValueExpressionHelper.createBareExpressionString(
                    widget.getValueName(), field);
            String iconPropertyValue = (String) widget.getProperty("icon");
            if (iconPropertyValue == null) {
                // Default fa icon
                iconPropertyValue = "check";
            }
            String faIcon = String.format(STYLECLASS_ICON_FORMAT, iconPropertyValue);
            String expression = String.format("#{%s ? '%s' : '%s'}",
                    bareExpression, faIcon, "");
            TagAttribute styleClassAttr = helper.createAttribute("styleClass", expression);
            attrs.add(styleClassAttr);
            TagAttribute valueAttr = helper.createAttribute("value", " ");
            attrs.add(valueAttr);
        }

        // fill with widget properties
        List<TagAttribute> propertyAttrs = helper.getTagAttributes(
                widget.getProperties(), null, true, widget.getType(),
                widget.getTypeCategory(), widget.getMode());
        if (propertyAttrs != null) {
            attrs.addAll(propertyAttrs);
        }
        TagAttributes widgetAttrs = FaceletHandlerHelper.getTagAttributes(attrs);
        // handle id
        if (!addId) {
            return widgetAttrs;
        } else {
            return FaceletHandlerHelper.addTagAttribute(widgetAttrs,
                    helper.createAttribute("id", id));
        }

    }
}
