package svg;

import org.robovm.apple.foundation.NSObject;
import org.robovm.apple.uikit.UIColor;
import org.robovm.objc.annotation.Method;

public interface SVGContext {
    @Method(selector = "colorForSVGColorString:")
    UIColor colorForSVGColorString(String svgColorString);

    @Method(selector = "objectAtURL:")
    NSObject objectAtURL(String aLocation);
}
