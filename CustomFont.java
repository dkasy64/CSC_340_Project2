import java.io.InputStream;
import java.awt.Font;

public class CustomFont {
    
    public static Font loadCustomFont(String fontPath) {
        try {
            //InputStream is = CustomFont.class.getResourceAsStream("/font/x12y16pxMaruMonica.ttf");
            //InputStream is = CustomFont.class.getResourceAsStream("/font/Buildingsandundertherailwaytracksfree_ver.otf");
            //InputStream is = CustomFont.class.getResourceAsStream("/font/natumemozi.ttf");
            //InputStream is = CustomFont.class.getResourceAsStream("/font/Kei_Ji.ttf");
            //InputStream is = CustomFont.class.getResourceAsStream("/font/Kei_Ji-P.ttf");
            InputStream is = CustomFont.class.getResourceAsStream("/font/x12y12pxMaruMinyaM.ttf");

            if (is != null) {
                return Font.createFont(Font.TRUETYPE_FONT, is);
            } else {
                System.out.println("Font not found");
            }
        } catch (Exception e) { 
            e.printStackTrace();
        }
        return new Font("SansSerif", Font.PLAIN, 12);
    }
}
