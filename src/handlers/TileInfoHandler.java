package handlers;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.File;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;

//tb/160404

/*
TileInfoHandler:
-takes pathinfo of request (/z/x/y) without parameters
-creates png image, 256x256 pixels, with border, with transparent background
-writes path components on image

$ cat WebServer.properties 
handler_classname=handlers.TileInfoHandler
port_range_start=8081
instance_count=4
context=/ti

use TileInfoHandler as layer in OpenLayers 2.x (JavaScript):

//create 4 urls to TileInfoHandler service running on localhost, starting at port 8081
var port_range_start=8081;
var tile_info_urls=[];
for(var i=0;i<4;i++)
{
	tile_info_urls.push("http://127.0.0."+(i+1)+":"+(port_range_start+i)+"/ti/${z}/${x}/${y}")
}

var layer_options=
{
	isBaseLayer: false //since the generated tiles have a transparent background, overlaying on a baselayer is possible
	//, more options
};

var tile_info_layer = new OpenLayers.Layer.OSM("tile info"
	,tile_info_urls
	,layer_options
);

*/

//========================================================================
public class TileInfoHandler extends AbstractHandler
{
	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException,ServletException
	{
		String pathInfo = request.getPathInfo();
		System.err.println(pathInfo);
		String tokens[]=pathInfo.split("/"); //"/z/x/y"
		if(tokens.length!=4)
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}
		String image_text="z: "+tokens[1]+"\nx: "+tokens[2]+"\ny: "+tokens[3];
		TextToGraphics ttg=new TextToGraphics();
		//public BufferedImage renderString(String text, String fontName, int fontSize, int fontStyle, Color fontColor, Color backgroundColor, int width, int height, int x, int y)
		BufferedImage img=ttg.renderString(image_text, "Arial", 24, Font.BOLD, Color.BLACK, null, 256, 256, 20, 20);
		response.setContentType("image/png");
		//if this is NOT set, tiles won't be displayed in OSM slippy map!
		response.setHeader("Access-Control-Allow-Origin", "*");
		ImageIO.write(img, "png", response.getOutputStream());
		baseRequest.setHandled(true);
	}//end handle()
}//end class TileInfoHandler

/*
ByteArrayOutputStream tmp = new ByteArrayOutputStream();
ImageIO.write(img, "png", tmp);
tmp.close();
Integer contentLength = tmp.size();
response.setHeader("Content-Length",contentLength.toString());
OutputStream out = response.getOutputStream();
out.write(tmp.toByteArray());
out.close();

PrintWriter out = response.getWriter();
out.println("<h1>" + greeting + "</h1>");
*/

//http://stackoverflow.com/questions/18800717/convert-text-content-to-image
//https://github.com/7890/jsnip ttg

//=============================================================================
//=============================================================================
class TextToGraphics
{
//=============================================================================
	public static void main(String[] args)
	{
		TextToGraphics ttg=new TextToGraphics();
		ttg.saveImageToFile(
			ttg.renderString("hello world\nfoo\nbar")
			,"out.png","png"
		);
	}

//=============================================================================
	public TextToGraphics(){}

//=============================================================================
	public void saveImageToFile(BufferedImage img, String filename, String filetype)
	{
		try
		{
			ImageIO.write(img, filetype, new File(filename));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

//=============================================================================
	public BufferedImage renderString(String text)
	{
		return renderString(text, "Arial", 24, Font.PLAIN, Color.BLACK, null, 0, 0, 0, 0);
	}

//=============================================================================
	public BufferedImage renderString(String text, String fontName, int fontSize, int fontStyle, Color fontColor, Color backgroundColor, int width, int height, int x, int y)
	{
		/*
		Because font metrics is based on a graphics context, we need to create
		a small, temporary image so we can ascertain the width and height
		of the final image
		*/
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = img.createGraphics();
		//Font font = new Font("Arial", Font.PLAIN, 48);
		Font font = new Font(fontName, fontStyle, fontSize);
		g2d.setFont(font);
		FontMetrics fm = g2d.getFontMetrics();

		if(width==0) //auto
		{
			width = fm.stringWidth(text);
		}
		if(height==0) //auto
		{
			height = fm.getHeight();
		}
		g2d.dispose();

		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		g2d = img.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setFont(font);
		fm = g2d.getFontMetrics();

		if(backgroundColor!=null)
		{
			g2d.setColor(backgroundColor);
			g2d.fillRect(0, 0, width, height);
		}
		g2d.setColor(fontColor);
		g2d.draw(new Rectangle2D.Double(0,0,width,height));

		//white background below text, "double"-text
		int offset=2;
		int y_orig=y;
		g2d.setColor(Color.WHITE);
		for(String line : text.split("\n"))
		{
			g2d.drawString(line, x + offset, ( y += fm.getHeight()) + offset );
		}
		y=y_orig;

		g2d.setColor(fontColor);
		//g2d.drawString(text, 0, fm.getAscent());
		for(String line : text.split("\n"))
		{
			g2d.drawString(line, x, y += fm.getHeight());
		}
		g2d.dispose();
		return img;
	}
}//end class TextToGraphics
//EOF