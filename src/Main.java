import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import Graphics.GLDisplay;
import Graphics.GLWindow;
import Graphics.Renderer;
import Utility.Vector2i;
import Utility.Vector3i;

public class Main extends GLDisplay
{
	public static final double SENSITIVITY = 0.05, ANIMATION_RATE = 0.2;
	
	public static Cube cube;
	public static Cube newCube;
	
	public static volatile double horAngle, verAngle;
	public static volatile double camZ = 8;
	
	public static volatile HashMap<Vector3i, Vector2i> tileMap = new HashMap<Vector3i, Vector2i>(); //Stores the xy coordinates (value) of the tiles drawn with their original xyz 3d coordinate (key)
	public static volatile HashMap<Vector3i, Vector2i> newTileMap = new HashMap<Vector3i, Vector2i>();
	
	public static volatile HashSet<Vector3i> tiles = new HashSet<Vector3i>();
	public static volatile HashMap<Vector3i, Vector3i[]> tileCoords = new HashMap<Vector3i, Vector3i[]>();
	public static volatile boolean animating;
	public static volatile double turnAngle, orientation; //Stores the angle by which tiles must be rotated (Used in animation)
	public static Vector3i axis; //Stores the axis about which those tiles must be rotated
	
	public static ArrayList<Vector2i> path = new ArrayList<Vector2i>();
	public static Vector2i start = Vector2i.NULL_VECTOR, end = Vector2i.NULL_VECTOR;
	
	public static Vector3i startTile = new Vector3i(), endTile = new Vector3i();
	
	public static GLWindow window;
	
	public static void main(String[] args)
	{
		cube = new Cube();
		
		Main main = new Main();
		window = new GLWindow();
		System.out.println("Starting window");
		window.start(main);
		
		double lastTime = System.nanoTime();
		double last = System.currentTimeMillis();
		//double delta = 0;
		double msPerTick = 1000.0 / 60;
		
		int frames = 0;
		int ups = 0;
		
		while (true)
		{
			double currentTime = System.nanoTime();
			//delta += (currentTime - lastTime) / nsPerTick;
			lastTime = currentTime;
			//while (delta >= 1)
			//{
			update();
			//delta--;
			ups++;
			//}
			if (System.currentTimeMillis() - last >= 1000)
			{
				window.frame.setTitle("UPS : " + ups);
				ups = 0;
				last += 1000;
			}
			try
			{
				Thread.sleep((long) msPerTick);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static void update()
	{
		if (window.keys[KeyEvent.VK_W] && camZ > 1.0)
			camZ -= 0.1;
		else if (window.keys[KeyEvent.VK_S] && camZ < 10.0)
			camZ += 0.1;
		
		//Camera rotation
		if (window.mousePressed[MouseEvent.BUTTON3] || window.mousePressed[MouseEvent.BUTTON2]) //Right-click or mmb click
		{
			horAngle += (double) (window.mouseX - window.prevMouseX) * SENSITIVITY / 10;
			verAngle += (double) (window.mouseY - window.prevMouseY) * SENSITIVITY / 10;
		}
		
		//Zoom
		camZ += window.mouseScroll;
		
		//Path drawing
		if (!animating)
		{
			if (window.mousePressed[MouseEvent.BUTTON1])
			{
				if (!window.prevMousePressed[MouseEvent.BUTTON1]) //If user is pressing mouse for the first time again, reset start, end and path array
				{
					start = Vector2i.NULL_VECTOR;
					end = Vector2i.NULL_VECTOR;
					path = new ArrayList<Vector2i>();
				}
				path.add(new Vector2i(window.mouseX, window.mouseY));
			}
			else if (window.prevMousePressed[MouseEvent.BUTTON1])
			{
				//Converting random path drawn by mouse to a 2d vector
				start = path.get(0); //Starting point of our vector will be same as starting point of mouse drawing
				end = Vector2i.NULL_VECTOR; //End point of our vector will be the avg of all the points drawn
				for (int i = 0; i < path.size(); i++)
					end = end.add(path.get(i));
				end = end.divide(path.size());
				
				//Converting start and end java coordinates to opengl coordinates (normalized to the range -1 to 1)
				Vector2i start = new Vector2i((Main.start.x - window.WIDTH / 2) / (window.WIDTH / 2), (window.HEIGHT / 2 - Main.start.y) / (window.HEIGHT / 2));
				Vector2i end = new Vector2i((Main.end.x - window.WIDTH / 2) / (window.WIDTH / 2), (window.HEIGHT / 2 - Main.end.y) / (window.HEIGHT / 2));
				
				//Basically, we find the closest tile to 'start' and 'end' vectors and pass the tile coordinates to cube class to carry out the actual rotation we want
				Vector3i startTile = new Vector3i();
				double minDist = Double.POSITIVE_INFINITY;
				for (Map.Entry<Vector3i, Vector2i> entry : tileMap.entrySet())
				{
					Vector2i centre = entry.getValue();
					double dist = centre.subtract(start).getMagnitude();
					if (dist < minDist)
					{
						minDist = dist;
						startTile = entry.getKey();
					}
				}
				Vector3i endTile = new Vector3i();
				minDist = Double.POSITIVE_INFINITY;
				for (Map.Entry<Vector3i, Vector2i> entry : tileMap.entrySet())
				{
					Vector2i centre = entry.getValue();
					double dist = centre.subtract(end).getMagnitude();
					if (dist < minDist)
					{
						minDist = dist;
						endTile = entry.getKey();
					}
				}
				turn(startTile, endTile);
				Main.startTile = startTile;
				Main.endTile = endTile;
			}
			if (window.keys[KeyEvent.VK_CONTROL]) //Undoes the last move done by user (Only works for one move. Doing undo again will just redo that one move)
			{
				if (window.keys[KeyEvent.VK_Z] && !window.previousKeys[KeyEvent.VK_Z])
				{
					turn(endTile, startTile);
					Vector3i temp = startTile.clone();
					startTile = endTile.clone();
					endTile = temp;
				}
			}
		}
		if (window.keys[KeyEvent.VK_SPACE] && !window.previousKeys[KeyEvent.VK_SPACE])
			cube.rotateRing(new Vector3i(2, 4, 2), new Vector3i(0, 1, 0), new Vector3i(-1, 0, 0), 1);
		
		//Resetting keyboard/mouse variables in window
		window.previousKeys = window.keys.clone();
		window.prevMousePressed = window.mousePressed.clone();
		window.prevMouseX = window.mouseX;
		window.prevMouseY = window.mouseY;
		window.mouseScroll = 0;
	}
	
	public static void turn(Vector3i startTile, Vector3i endTile)
	{
		newCube = new Cube();
		newCube.tiles = Cube.clone(cube.tiles);
		newCube.turn(startTile, endTile);
		animating = true;
	}
	
	@Override
	public void init(GLAutoDrawable drawable, int WIDTH, int HEIGHT)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void display(GLAutoDrawable drawable, int WIDTH, int HEIGHT)
	{
		GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(135.0f / 255, 206.0f / 255, 235.0f / 255, 1.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		//gl.glTranslatef(0, 0, -1.0f);
		
		//Drawing faces (Faces are named in comments as seen from camera pointed towards -ve Z axis)
		Vector3i[][] faces = new Vector3i[6][3];
		faces[0] = new Vector3i[] {new Vector3i(2, 2, 4), new Vector3i(0, 0, 1), new Vector3i(1, 0, 0)}; //Front-face coords
		faces[1] = new Vector3i[] {new Vector3i(2, 2, 0), new Vector3i(0, 0, -1), new Vector3i(1, 0, 0)}; //Back-face coords
		faces[2] = new Vector3i[] {new Vector3i(4, 2, 2), new Vector3i(1, 0, 0), new Vector3i(0, 0, 1)}; //Right-face
		faces[3] = new Vector3i[] {new Vector3i(0, 2, 2), new Vector3i(-1, 0, 0), new Vector3i(0, 0, 1)}; //Left-face
		faces[4] = new Vector3i[] {new Vector3i(2, 4, 2), new Vector3i(0, 1, 0), new Vector3i(1, 0, 0)}; //Top-face
		faces[5] = new Vector3i[] {new Vector3i(2, 0, 2), new Vector3i(0, -1, 0), new Vector3i(1, 0, 0)}; //Bottom-face
		
		//Arranging faces in order of distance from Z-axis, after they are rotated by camera's angles
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 6 - i - 1; j++)
			{
				double d1 = camZ - faces[j][0].rotateY(horAngle).rotateX(verAngle).z, d2 = camZ - faces[j + 1][0].rotateY(horAngle).rotateX(verAngle).z;
				if (d1 < d2) //We want the faces closest to the cam to be drawn last, so we want to arrange faces in descending order of z-value
				{
					Vector3i[] temp = faces[j];
					faces[j] = faces[j + 1];
					faces[j + 1] = temp;
				}
			}
		}
		
		//Only 3 faces of a cube are visible at a time, so we can skip drawing the first 3 and simply draw the closest 3
		//However, we'll draw the 4th face as well since otherwise we get rendering issue when cube rotation animation takes place
		int i;
		if (animating) //Only if we're animating we want to draw all tiles. Otherwise we just draw the visible tiles. This way we don't have to bother with the non-visible tiles messing with user's input
			i = 0;
		else
			i = 3;
		for (; i < 6; i++)
		{
			drawFace(faces[i][0], faces[i][1], faces[i][2], gl);
		}
		
		renderAll(gl);
		
		tileMap = (HashMap<Vector3i, Vector2i>) newTileMap.clone();
		newTileMap.clear();
		
		if (animating)
		{
			turnAngle += ANIMATION_RATE * orientation;
			if (turnAngle >= Math.PI / 2 || turnAngle <= -Math.PI / 2)
			{
				animating = false;
				cube = newCube;
				turnAngle = 0;
				tiles.clear();
			}
		}
		
		//Draws red line indicating the 2d vector formed by our mouse drag
		/*Renderer.useColor(0xFFFF0000, gl);
		gl.glBegin(GL2.GL_LINES);
		gl.glVertex2d((start.x - WIDTH / 2) / (WIDTH / 2), (HEIGHT / 2 - start.y) / (HEIGHT / 2));
		gl.glVertex2d((end.x - WIDTH / 2) / (WIDTH / 2), (HEIGHT / 2 - end.y) / (HEIGHT / 2));
		gl.glEnd();*/
		
		tileCoords.clear();
	}
	
	public void drawFace(Vector3i centre, Vector3i normal, Vector3i perp, GL2 gl)
	{
		//Drawing the face by taking 3 tiles at the edge, 4 times, that is, top 3 tiles, right-side 3 tiles, bottom 3 tiles and left 3 tiles
		
		Vector3i lineCentre = perp; //Position of the middle tile among the 3 tiles wrt pos
		Vector3i dir = normal.cross(perp); //Position of tile to the right of the middle tile
		
		//Drawing the tiles themselves
		for (int i = 0; i < 4; i++)
		{
			Vector3i rightTile = centre.add(lineCentre).add(dir).round();
			Vector3i midTile = centre.add(lineCentre).round();
			Vector3i leftTile = centre.add(lineCentre).subtract(dir).round();
			
			fillTile(rightTile, normal, perp, cube.tiles[(int) rightTile.x][(int) rightTile.y][(int) rightTile.z], gl);
			fillTile(midTile, normal, perp, cube.tiles[(int) midTile.x][(int) midTile.y][(int) midTile.z], gl);
			fillTile(leftTile, normal, perp, cube.tiles[(int) leftTile.x][(int) leftTile.y][(int) leftTile.z], gl);
			
			lineCentre = lineCentre.rotateAbout(normal, Math.toRadians(90));
			dir = dir.rotateAbout(normal, Math.toRadians(90));
		}
		
		//Drawing the centre tile
		fillTile(centre, normal, perp, cube.tiles[(int) centre.x][(int) centre.y][(int) centre.z], gl);
		
		//Drawing borders around the tiles
		lineCentre = perp; //Position of the middle tile among the 3 tiles wrt pos
		dir = normal.cross(perp); //Position of tile to the right of the middle tile
		for (int i = 0; i < 4; i++)
		{
			Vector3i rightTile = centre.add(lineCentre).add(dir).round();
			Vector3i midTile = centre.add(lineCentre).round();
			Vector3i leftTile = centre.add(lineCentre).subtract(dir).round();
			
			drawTile(rightTile, normal, perp, 0, gl);
			drawTile(midTile, normal, perp, 0, gl);
			drawTile(leftTile, normal, perp, 0, gl);
			
			lineCentre = lineCentre.rotateAbout(normal, Math.toRadians(90));
			dir = dir.rotateAbout(normal, Math.toRadians(90));
		}
		
		drawTile(centre, normal, perp, 0, gl);
	}
	
	//Draws a square of size 1 with centre at 'centre' vector and in the plane parallel to 'perp'
	//NOTE : 'perp' vector must be a vector pointing from centre to any one side of the square
	public void fillTile(Vector3i centre, Vector3i normal, Vector3i perp, int col, GL2 gl)
	{
		renderTile(centre, normal, perp, col, RENDER_TYPE.FILL, gl);
	}
	
	public void drawTile(Vector3i centre, Vector3i normal, Vector3i perp, int col, GL2 gl)
	{
		renderTile(centre, normal, perp, col, RENDER_TYPE.DRAW, gl);
	}
	
	//THIS FUNCTION JUST RENDERS THE FRAMES (BLACK LINES) OF THE CUBE. THE ACTUAL TILES ARE ADDED TO A HASHMAP, WHICH WILL BE SORTED BY Z-VALUE AND THEN RENDERED ONE BY ONE
	//We need to do this because tiles don't get properly rendered when cube rotation animation is going on.
	public void renderTile(Vector3i centreOriginal, Vector3i normal, Vector3i perp, int col, RENDER_TYPE type, GL2 gl)
	{
		//Since our made up coordinate system does not store the actual coordinates of the tiles, the faces of the cube are drawn detached from each other
		//(with a gap of 0.5 units in each axis) (Remove this code if you want to see what I mean)
		//So basically, we have to shift centre 0.5 units towards (opposite to direction pointed by) the normal vector
		Vector3i centre = centreOriginal.add(normal.multiply(-0.5));
		
		normal = normal.normalize();
		perp = perp.normalize();
		double sqrt = Math.sqrt(2);
		perp = perp.rotateAbout(normal, Math.toRadians(45));
		Vector3i v1 = centre.add(perp.divide(sqrt)); //We divide by sqrt(2) since 1/2 * length of diagonal of a square (Dist btwn centre to one vertex) = 1/2 * sqrt(2) * s = 1/sqrt(2) since side = 1
		perp = perp.rotateAbout(normal, Math.toRadians(90));
		Vector3i v2 = centre.add(perp.divide(sqrt));
		perp = perp.rotateAbout(normal, Math.toRadians(90));
		Vector3i v3 = centre.add(perp.divide(sqrt));
		perp = perp.rotateAbout(normal, Math.toRadians(90));
		Vector3i v4 = centre.add(perp.divide(sqrt));
		
		//Centering the vertices [Since all our tiles' coordinates are positive]
		v1 = v1.subtract(cube.mid);
		v2 = v2.subtract(cube.mid);
		v3 = v3.subtract(cube.mid);
		v4 = v4.subtract(cube.mid);
		
		if (animating)
		{
			if (tiles.contains(centreOriginal))
			{
				v1 = v1.rotateAbout(axis, turnAngle);
				v2 = v2.rotateAbout(axis, turnAngle);
				v3 = v3.rotateAbout(axis, turnAngle);
				v4 = v4.rotateAbout(axis, turnAngle);
			}
		}
		
		//Rotating the vertices depending on camera's angles
		v1 = v1.rotateY(horAngle);
		v2 = v2.rotateY(horAngle);
		v3 = v3.rotateY(horAngle);
		v4 = v4.rotateY(horAngle);
		
		v1 = v1.rotateX(verAngle);
		v2 = v2.rotateX(verAngle);
		v3 = v3.rotateX(verAngle);
		v4 = v4.rotateX(verAngle);
		
		//Converting the coords to 2d by dividing by z-value [z-distance between camera and point]
		double v1z = camZ - v1.z, v2z = camZ - v2.z, v3z = camZ - v3.z, v4z = camZ - v4.z;
		v1 = v1.divide(camZ - v1.z); //Since camera is at camZ, distance will be camZ-v1.z
		v2 = v2.divide(camZ - v2.z);
		v3 = v3.divide(camZ - v3.z);
		v4 = v4.divide(camZ - v4.z);
		
		//These z values will be used in renderAll() function to arrange the tiles in order, so we need to set them back as we'll put these in the HashMap as Vector3is
		if (type == RENDER_TYPE.FILL)
		{
			v1.z = v1z;
			v2.z = v2z;
			v3.z = v3z;
			v4.z = v4z;
			tileCoords.put(centreOriginal, new Vector3i[] {v1, v2, v3, v4, new Vector3i(col, 0, 0)}); //We also need to send col of tile to the renderAll() function, so we make a new vector3i to do that
		}
		
		/*Renderer.useColor(col, gl);
		if (type == RENDER_TYPE.DRAW)
			gl.glBegin(GL2.GL_LINE_STRIP);
		//else if (type == RENDER_TYPE.FILL)
		//gl.glBegin(GL2.GL_POLYGON);
		gl.glVertex3d(v1.x, v1.y, v1.z);
		gl.glVertex3d(v2.x, v2.y, v2.z);
		gl.glVertex3d(v3.x, v3.y, v3.z);
		gl.glVertex3d(v4.x, v4.y, v4.z);
		gl.glVertex3d(v1.x, v1.y, v1.z);
		gl.glEnd();*/
		
		//Centre of tile on our 2D screen (Will be used for figuring user input)
		//We also pass the z-value so that user's input can be accurately determined (Otherwise it's possible a tile gets selected that's not even visible to the user)
		Vector2i centre2D = (v1.add(v2).add(v3).add(v4)).toVector2i().divide(4);
		newTileMap.put(centreOriginal, centre2D);
	}
	
	public void renderAll(GL2 gl)
	{
		ArrayList<Vector3i[]> tiles = new ArrayList<Vector3i[]>();
		for (Map.Entry<Vector3i, Vector3i[]> entry : tileCoords.entrySet())
		{
			tiles.add(entry.getValue());
		}
		
		//Sorts the 'tiles' arrayList according to the z values
		Collections.sort(tiles, (arr1, arr2) -> ((int) Math.signum(((arr2[0].z + arr2[1].z + arr2[2].z + arr2[3].z) / 4 - (arr1[0].z + arr1[1].z + arr1[2].z + arr1[3].z) / 4))));
		
		for (int i = 0; i < tiles.size(); i++)
		{
			Vector3i v1 = tiles.get(i)[0];
			Vector3i v2 = tiles.get(i)[1];
			Vector3i v3 = tiles.get(i)[2];
			Vector3i v4 = tiles.get(i)[3];
			int col = (int) tiles.get(i)[4].x;
			Renderer.useColor(col, gl);
			gl.glBegin(GL2.GL_POLYGON);
			gl.glVertex3d(v1.x, v1.y, 1);
			gl.glVertex3d(v2.x, v2.y, 1);
			gl.glVertex3d(v3.x, v3.y, 1);
			gl.glVertex3d(v4.x, v4.y, 1);
			gl.glVertex3d(v1.x, v1.y, 1);
			gl.glEnd();
			
			Renderer.useColor(0, gl);
			gl.glBegin(GL2.GL_LINE_STRIP);
			gl.glVertex3d(v1.x, v1.y, 1);
			gl.glVertex3d(v2.x, v2.y, 1);
			gl.glVertex3d(v3.x, v3.y, 1);
			gl.glVertex3d(v4.x, v4.y, 1);
			gl.glVertex3d(v1.x, v1.y, 1);
			gl.glEnd();
		}
	}
	
	enum RENDER_TYPE
	{
		DRAW, FILL;
	}
}
