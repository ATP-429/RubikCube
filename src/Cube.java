import Utility.Vector3i;

public class Cube
{
	int[][][] tiles = new int[5][5][5];
	//'obs' stands for observer. This vector points to the center tile of the face we're looking at rn.
	//'dir' stands for direction. This vector points to the right-side of our camera wrt the face we're looking at rn. That is, if we were to move right from our current location, we would have to move along this vector
	Vector3i obs = new Vector3i(2, 2, 4), dir = new Vector3i(2, 0, 0);//This means cam is originally pointed at front view of cube normally (As though we were just looking at the cube with our eyes standing on the ground)
	Vector3i mid = new Vector3i(2, 2, 2); //Points to the centre of the cube
	
	public Cube()
	{
		//Giving color to tiles in each face
		Vector3i[][] faces = new Vector3i[6][3];
		faces[0] = new Vector3i[] {new Vector3i(2, 2, 4), new Vector3i(0, 0, 1), new Vector3i(1, 0, 0)}; //Front-face coords
		faces[1] = new Vector3i[] {new Vector3i(2, 2, 0), new Vector3i(0, 0, -1), new Vector3i(1, 0, 0)}; //Back-face coords
		faces[2] = new Vector3i[] {new Vector3i(4, 2, 2), new Vector3i(1, 0, 0), new Vector3i(0, 0, 1)}; //Right-face
		faces[3] = new Vector3i[] {new Vector3i(0, 2, 2), new Vector3i(-1, 0, 0), new Vector3i(0, 0, 1)}; //Left-face
		faces[4] = new Vector3i[] {new Vector3i(2, 4, 2), new Vector3i(0, 1, 0), new Vector3i(1, 0, 0)}; //Top-face
		faces[5] = new Vector3i[] {new Vector3i(2, 0, 2), new Vector3i(0, -1, 0), new Vector3i(1, 0, 0)}; //Bottom-face
		//Orange, red, blue, green, yellow, white are the front, back, right, left, top and bottom faces respectively. 
		//The colors are arranged the same as an official cube, that is, red opposite orange, blue opposite green, and yellow opposite white
		int[] cols = new int[] {0xFFF05E23, 0xFFB5011E, 0xFF0072EC, 0xFF00A23A, 0xFFF6D900, 0xFFEFEFF7};
		
		for (int i = 0; i < 6; i++)
		{
			setFaceColor(faces[i][0], faces[i][1], faces[i][2], cols[i]);
		}
	}
	
	public void setFront()
	{
		
	}
	
	//Rotates the face whose centre is at 'pos' by 'count' number of rotations
	public void rotateFace(Vector3i pos, int count) //Positive count means counter-clockwise
	{
		Main.tiles.add(pos);
		count %= 4; //Since rotating a face 4 times is the same as not rotating it at all
		if (count < 0)
			count += 4; //If count is -1, for eg, it's the same as rotating the face 3 times in the other direction
		for (int i = 0; i < count; i++) //Rotating this face 'count' number of times
		{
			//Calculating required stuff
			Vector3i[] perps = new Vector3i[] {new Vector3i(0, 0, 1), new Vector3i(0, 1, 0), new Vector3i(1, 0, 0)}; //Stores all possibilities of perpendicular vectors to pos
			Vector3i relPos = pos.subtract(mid);
			Vector3i perp = (perps[0].dot(relPos) == 0) ? (perps[0]) : (perps[1].dot(relPos) == 0 ? perps[1] : (perps[2].dot(relPos) == 0 ? perps[2] : null)); //Atleast one vector MUST be perpendicular to the relPos vector, since the relPos vector is itself perpendicular to atleast one axis
			perp = perp.normalize().round();
			Vector3i perp2 = relPos.cross(perp).normalize().round(); //Vector perpendicular to perp and relPos, which means it points along the direction of the ring
			
			//Moving tiles on the edge, that is, the ring of tiles surrounding this face
			//Since we're rotating counter-clockwise, 'sign' should be positive. If it's negative, it means perp2 will rotate the cube in clockwise direction 
			//which would be wrong, so we multiply it by sign (now negative) to reverse its direction and make it right
			int sign = (int) Math.round(Math.cos(Vector3i.angleBetween(perp.cross(perp2), relPos)));
			Vector3i ringCentre = pos.add(perp.multiply(2)).subtract(relPos.normalize()).round();
			rotateRing(ringCentre, perp, perp2.multiply(sign), 1);
			
			int[][][] newTiles = clone(tiles);
			
			//Moving tiles on the face itself
			//There are four 3-tiled "lines" on one face, and one centre tile. We just have to shift the 3-tiled lines by 90 degrees
			Vector3i lineCentre = perp; //Position of the middle tile among the 3 tiles wrt pos
			Vector3i dir = perp2.round(); //Position of tile to the right of the middle tile
			for (int j = 0; j < 4; j++) //Since there are 4 lines
			{
				//Since we want to rotate this face counter-clockwise, tiles in the current line are given by tiles 90 degrees to the front (clockwise)
				Vector3i lineCentre2 = lineCentre.rotateAbout(relPos, -Math.toRadians(90)).round(); //Rotating lineCentre by 90 degrees clockwise
				Vector3i dir2 = dir.rotateAbout(relPos, -Math.toRadians(90)).round(); //Rotating dir by 90 degrees clockwise
				
				Vector3i rightTile = pos.add(lineCentre).add(dir).round();
				Vector3i midTile = pos.add(lineCentre).round();
				Vector3i leftTile = pos.add(lineCentre).subtract(dir).round();
				
				Vector3i rightTile2 = pos.add(lineCentre2).add(dir2).round();
				Vector3i midTile2 = pos.add(lineCentre2).round();
				Vector3i leftTile2 = pos.add(lineCentre2).subtract(dir2).round();
				
				//Shifting tiles from 90 degrees clockwise to the front to current location
				newTiles[(int) rightTile.x][(int) rightTile.y][(int) rightTile.z] = tiles[(int) rightTile2.x][(int) rightTile2.y][(int) rightTile2.z];
				newTiles[(int) midTile.x][(int) midTile.y][(int) midTile.z] = tiles[(int) midTile2.x][(int) midTile2.y][(int) midTile2.z];
				newTiles[(int) leftTile.x][(int) leftTile.y][(int) leftTile.z] = tiles[(int) leftTile2.x][(int) leftTile2.y][(int) leftTile2.z];
				
				Main.tiles.add(rightTile);
				Main.tiles.add(midTile);
				Main.tiles.add(leftTile);
				
				//Rotating lineCentre and dir by 90 degrees clockwise to set the next set of 3 tiles
				lineCentre = lineCentre2;
				dir = dir2;
			}
			
			tiles = clone(newTiles);
		}
	}
	
	//Rotates the ring which has one centre tile at 'pos', along the vector 'dir' by 'count' number of rotations
	public void rotateRing(Vector3i pos, Vector3i normal, Vector3i dir, int count)
	{
		count %= 4; //Since rotating a face 4 times is the same as not rotating it at all
		if (count < 0)
			count += 4; //If count is -1, for eg, it's the same as rotating the face 3 times in the other direction
		for (int i = 0; i < count; i++) //Rotating this face 'count' number of times along the vector 'dir'
		{
			int[][][] newTiles = clone(tiles);
			//There are four 3-tiled "lines" on one side of the ring, and one centre tile. We just have to shift the 3-tiled lines by 90 degrees
			Vector3i mid = pos.subtract(normal.multiply(2)); //mid-point of the ring
			Vector3i lineCentre = normal.multiply(2); //Position of centre tile wrt mid-point of ring
			Vector3i axis = lineCentre.cross(dir); //Axis about which we want to rotate lineCentre to go through all tiles of the ring
			for (int j = 0; j < 4; j++) //Since there are 4 lines
			{
				//Since we want to rotate this face, tiles in the current line are given by tiles 90 degrees phase difference
				//We rotate clockwise, as that happens to be the direction to rotate them in, in order to move the ring along the vector 'dir'
				Vector3i lineCentre2 = lineCentre.rotateAbout(axis, -Math.toRadians(90)).round(); //Rotating lineCentre by 90 degrees clockwise
				Vector3i dir2 = dir.rotateAbout(axis, -Math.toRadians(90)).round(); //Rotating dir by 90 degrees clockwise
				
				Vector3i rightTile = mid.add(lineCentre).add(dir).round();
				Vector3i midTile = mid.add(lineCentre).round();
				Vector3i leftTile = mid.add(lineCentre).subtract(dir).round();
				
				Vector3i rightTile2 = mid.add(lineCentre2).add(dir2).round();
				Vector3i midTile2 = mid.add(lineCentre2).round();
				Vector3i leftTile2 = mid.add(lineCentre2).subtract(dir2).round();
				
				//Shifting tiles from 90 degrees clockwise to the front to current location
				newTiles[(int) rightTile.x][(int) rightTile.y][(int) rightTile.z] = tiles[(int) rightTile2.x][(int) rightTile2.y][(int) rightTile2.z];
				newTiles[(int) midTile.x][(int) midTile.y][(int) midTile.z] = tiles[(int) midTile2.x][(int) midTile2.y][(int) midTile2.z];
				newTiles[(int) leftTile.x][(int) leftTile.y][(int) leftTile.z] = tiles[(int) leftTile2.x][(int) leftTile2.y][(int) leftTile2.z];
				
				Main.tiles.add(rightTile);
				Main.tiles.add(midTile);
				Main.tiles.add(leftTile);
				
				//Rotating lineCentre and dir by 90 degrees clockwise to set the next set of 3 tiles
				lineCentre = lineCentre2;
				dir = dir2;
			}
			tiles = clone(newTiles);
		}
	}
	
	public void moveRight()
	{
		//When we move right from a surface, the dir vector will become opposite to the vector pointing to obs from mid
		//Also, to get the new obs vector, we should simply move along the original dir vector once (which will bring us to the edge) and then moving along the new dir vector once
		Vector3i newDir = obs.subtract(mid).multiply(-1);
		obs = obs.add(dir);
		obs = obs.add(newDir);
		dir = newDir;
	}
	
	public void moveLeft()
	{
		//Same as moving right, when we move left from a surface, the dir vector will become opposite to the vector pointing to obs from mid
		//Also, to get the new obs vector, we should move opposite the original dir vector once and then move along the new dir vector once
		Vector3i newDir = obs.subtract(mid).multiply(-1);
		obs = obs.add(dir.multiply(-1));
		obs = obs.add(newDir);
		dir = newDir;
	}
	
	public void moveUp()
	{
		//When we move up, the dir vector remains pointing in the same direction
		//Also, to get new obs vector, we move 2 units along the cross product of dir and vector pointing to mid from obs, once, and then move opp to vector pointing to obs from mid, once
		Vector3i centre = obs.subtract(mid);
		Vector3i cross = dir.cross(centre.multiply(-1)).normalize().multiply(2).round();
		obs = obs.add(cross);
		obs = obs.add(centre.multiply(-1));
	}
	
	void moveDown()
	{
		//Same as moving up, when we move down, the dir vector remains pointing in the same direction
		//Also, to get new obs vector, we move 2 units opp the cross product of dir and vector pointing to mid from obs once, and then move opp to vector pointing to obs from mid, once
		Vector3i centre = obs.subtract(mid);
		Vector3i cross = dir.cross(centre.multiply(-1)).normalize().multiply(2).round();
		obs = obs.add(cross.multiply(-1));
		obs = obs.add(centre.multiply(-1));
	}
	
	public void rotateClockwise()
	{
		dir = dir.rotateAbout(obs.subtract(mid), -Math.toRadians(90)).round();
	}
	
	public void rotateCounterclockwise()
	{
		dir = dir.rotateAbout(obs.subtract(mid), Math.toRadians(90)).round();
	}
	
	public void turn(Vector3i start, Vector3i end)
	{
		if (!start.equals(end))
		{
			Vector3i relStart = start.subtract(mid);
			Vector3i relEnd = end.subtract(mid);
			//If start and end, wrt centre of cube, are in one of xy, yz, zx plane, that is, x = k, y = k, z = k, where -1 <= k <= 1 (otherwise two tiles on one face will be counted, since k = 2 in that case)
			//then our operation is a valid ring turn.
			if (relStart.x == relEnd.x && Math.abs(relStart.x) <= 1) //Ring in the x = k plane
			{
				Vector3i plane = new Vector3i(relStart.x, 0, 0); //Vector perpendicular to x = k plane, and pointing to that plane from 'mid'. Also centre of our ring wrt mid
				Vector3i s = relStart.subtract(plane); //Vector pointing to start from centre of ring
				Vector3i e = relEnd.subtract(plane); //Vector pointing to end from centre of ring
				int orientation = (int) Math.round(Math.cos(Vector3i.angleBetween(s.cross(e), plane))); //-1 if ring rotation is clockwise, 1 if ring rotation is anti-clockwise
				Vector3i dir = new Vector3i();
				if (relStart.x < 0) //Ring we want to rotate is on the left side
				{
					rotateFace(new Vector3i(0, 2, 2), orientation);
					Main.axis = plane;
					Main.orientation = orientation;
				}
				else if (relStart.x > 0) //Ring on right side
				{
					rotateFace(new Vector3i(4, 2, 2), orientation);
					Main.axis = plane;
					Main.orientation = orientation;
				}
				else //Ring in centre
				{
					Vector3i axis = new Vector3i(1, 0, 0);
					orientation = (int) Math.round(Math.cos(Vector3i.angleBetween(s.cross(e), axis))); //-1 if ring rotation is clockwise, 1 if ring rotation is anti-clockwise
					dir = new Vector3i(0, 0, orientation);
					rotateRing(new Vector3i(2, 4, 2), new Vector3i(0, 1, 0), dir, 1); //Picking a tile in the top face to rotate this ring
					Main.axis = axis;
					Main.orientation = orientation;
				}
			}
			else if (relStart.y == relEnd.y && Math.abs(relStart.y) <= 1) //Ring in the y = k plane
			{
				Vector3i plane = new Vector3i(0, relStart.y, 0); //Vector perpendicular to y = k plane, and pointing to that plane from 'mid'. Also centre of our ring wrt mid
				Vector3i s = relStart.subtract(plane); //Vector pointing to start from centre of ring
				Vector3i e = relEnd.subtract(plane); //Vector pointing to end from centre of ring
				int orientation = (int) Math.round(Math.cos(Vector3i.angleBetween(s.cross(e), plane))); //-1 if ring rotation is clockwise, 1 if ring rotation is anti-clockwise
				Vector3i dir = new Vector3i();
				if (relStart.y < 0) //Ring we want to rotate is on the bottom
				{
					rotateFace(new Vector3i(2, 0, 2), orientation);
					Main.axis = plane;
					Main.orientation = orientation;
				}
				else if (relStart.y > 0) //Ring on top
				{
					rotateFace(new Vector3i(2, 4, 2), orientation);
					Main.axis = plane;
					Main.orientation = orientation;
				}
				else //Ring in centre
				{
					Vector3i axis = new Vector3i(0, 1, 0);
					orientation = (int) Math.round(Math.cos(Vector3i.angleBetween(s.cross(e), axis))); //-1 if ring rotation is clockwise, 1 if ring rotation is anti-clockwise
					dir = new Vector3i(orientation, 0, 0);
					rotateRing(new Vector3i(2, 2, 4), new Vector3i(0, 0, 1), dir, 1); //Picking a tile in the front face
					Main.axis = axis;
					Main.orientation = orientation;
				}
			}
			else if (relStart.z == relEnd.z && Math.abs(relStart.z) <= 1) //Ring in the z = k plane
			{
				Vector3i plane = new Vector3i(0, 0, relStart.z); //Vector perpendicular to z = k plane, and pointing to that plane from 'mid'. Also centre of our ring wrt mid
				Vector3i s = relStart.subtract(plane); //Vector pointing to start from centre of ring
				Vector3i e = relEnd.subtract(plane); //Vector pointing to end from centre of ring
				int orientation = (int) Math.round(Math.cos(Vector3i.angleBetween(s.cross(e), plane))); //-1 if ring rotation is clockwise, 1 if ring rotation is anti-clockwise
				Vector3i dir = new Vector3i();
				if (relStart.z < 0) //Ring we want to rotate is at the back
				{
					rotateFace(new Vector3i(2, 2, 0), orientation);
					Main.axis = plane;
					Main.orientation = orientation;
				}
				else if (relStart.z > 0) //Ring in front
				{
					rotateFace(new Vector3i(2, 2, 4), orientation);
					Main.axis = plane;
					Main.orientation = orientation;
				}
				else //Ring in centre
				{
					Vector3i axis = new Vector3i(0, 0, 1);
					orientation = (int) Math.round(Math.cos(Vector3i.angleBetween(s.cross(e), axis))); //-1 if ring rotation is clockwise, 1 if ring rotation is anti-clockwise
					dir = new Vector3i(-orientation, 0, 0);
					rotateRing(new Vector3i(2, 4, 2), new Vector3i(0, 1, 0), dir, 1); //Picking a tile at the top face
					Main.axis = axis;
					Main.orientation = orientation;
				}
			}
		}
	}
	
	public void setFaceColor(Vector3i centre, Vector3i normal, Vector3i perp, int col)
	{
		//CODE TAKEN (AND ADJUSTED TO FIT OUR NEEDS) FROM Main drawFace() function
		
		//Setting colors in the face by taking 3 tiles at the edge, 4 times, that is, top 3 tiles, right-side 3 tiles, bottom 3 tiles and left 3 tiles
		Vector3i lineCentre = perp; //Position of the middle tile among the 3 tiles wrt pos
		Vector3i dir = normal.cross(perp); //Position of tile to the right of the middle tile
		
		//Setting colors in the tiles themselves
		for (int i = 0; i < 4; i++)
		{
			Vector3i rightTile = centre.add(lineCentre).add(dir).round();
			Vector3i midTile = centre.add(lineCentre).round();
			Vector3i leftTile = centre.add(lineCentre).subtract(dir).round();
			tiles[(int) rightTile.x][(int) rightTile.y][(int) rightTile.z] = col;
			tiles[(int) midTile.x][(int) midTile.y][(int) midTile.z] = col;
			tiles[(int) leftTile.x][(int) leftTile.y][(int) leftTile.z] = col;
			
			lineCentre = lineCentre.rotateAbout(normal, Math.toRadians(90));
			dir = dir.rotateAbout(normal, Math.toRadians(90));
		}
		
		tiles[(int) centre.x][(int) centre.y][(int) centre.z] = col;
	}
	
	public static int[][][] clone(int[][][] tiles)
	{
		int[][][] cloned = new int[5][5][5];
		for (int i = 0; i < 5; i++)
		{
			for (int j = 0; j < 5; j++)
			{
				for (int k = 0; k < 5; k++)
				{
					cloned[i][j][k] = tiles[i][j][k];
				}
			}
		}
		
		return cloned;
	}
}
