package com.leverage.gameobjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ShortArray;
import com.leverage.gameobjects.Ball;
import com.leverage.gameobjects.Walls;
import com.leverage.jbHelpers.AssetLoader;

import java.util.Iterator;

public class LevelMap {
	//массив стенок
    private Array<Walls> walls = new Array();
    private Array<PolygonSprite> polySpriteArray = new Array();
    private Array<Sprite> spriteArray = new Array();
    Walls myWall;
	//радиус стенок
    int borderDefR = 5;

    public LevelMap() {
        this.clearArrays();
        this.borderWalls();
        this.myWall = new Walls(30, 200, 5, 200, 45);
        this.walls.add(this.myWall);
        this.polySpriteCreation();
    }

	//стенки по границам поля
    public void borderWalls() {
        float screenWidth = (float)Gdx.graphics.getWidth();
        float screenHeight = (float)Gdx.graphics.getHeight();
        short gameWidth = 640;
        float gameHeight = screenHeight / (screenWidth / (float)gameWidth);
        this.myWall = new Walls(0, 0, this.borderDefR, gameWidth, 0.0F);
        this.walls.add(this.myWall);
        this.myWall = new Walls(0, 0, this.borderDefR, (int)gameHeight, 90.0F);
        this.walls.add(this.myWall);
        this.myWall = new Walls(gameWidth, 0, this.borderDefR, (int)gameHeight, 90.0F);
        this.walls.add(this.myWall);
        this.myWall = new Walls(0, (int)gameHeight, this.borderDefR, gameWidth, 0.0F);
        this.walls.add(this.myWall);
    }

	//удаление стенок
    public void clearArrays() {
        this.walls.clear();
        this.polySpriteArray.clear();
        this.spriteArray.clear();
    }

	//проверка касания шайбы и стенок
    public void update(Ball ball) {
        Iterator var2 = this.walls.iterator();

        while(var2.hasNext()) {
            Walls wall = (Walls)var2.next();
            if(!wall.isBallInWall()) {
                if(wall.Collides(ball)) {
                    wall.changeBallInWall();
                    Gdx.app.log("ballInWall nubmer " + Integer.toString(this.walls.indexOf(wall, true)) + ": ", Boolean.toString(wall.isBallInWall()));
                    Gdx.app.log(wall.getStartWallPoint().toString(), wall.getEndWallPoint().toString());
					//шайба отскакивает от стенки
                    ball.onClick(wall);
                }
            } else if(!wall.Collides(ball)) {
                wall.changeBallInWall();
                Gdx.app.log("ballInWall nubmer " + Integer.toString(this.walls.indexOf(wall, true)) + ": ", Boolean.toString(wall.isBallInWall()));
            }
        }

    }

	//создание спрайтов
    public void polySpriteCreation() {
        Pixmap pix = new Pixmap(1, 1, Format.RGBA8888);
        pix.setColor(Color.BLACK);
        pix.fill();
        Texture textureSolid = new Texture(pix);
        TextureRegion textureRegion = new TextureRegion(textureSolid);
        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        Iterator var7 = this.walls.iterator();
        int numberOfWall = 0;
        Sprite wallSprite;
        while(var7.hasNext()) {
            Walls wall = (Walls)var7.next();
            float[] vertices = wall.getVert();
            ShortArray triangleIndices = triangulator.computeTriangles(vertices);
            PolygonRegion polyReg;
            PolygonSprite polySprite;
            if (numberOfWall < 4){
                polyReg = new PolygonRegion(textureRegion, vertices, triangleIndices.toArray()); //для границ
                polySprite = new PolygonSprite(polyReg);
                this.polySpriteArray.add(polySprite);
            } else {
                wallSprite = new Sprite(AssetLoader.wallMain);
                wallSprite.setBounds(wall.getStartWallPoint().x,wall.getStartWallPoint().y-(wall.getHeight()/2), wall.getWidth(), wall.getHeight());
                wallSprite.setOrigin(0,wall.getHeight()/2);
                wallSprite.setRotation(wall.getAngle());
                this.spriteArray.add(wallSprite);
                //закругления стенок:
                //startwall
                wallSprite = new Sprite(AssetLoader.wallEdge);
                wallSprite.setBounds(wall.getStartWallPoint().x-(wall.getHeight()/2),wall.getStartWallPoint().y-(wall.getHeight()/2), wall.getHeight(), wall.getHeight());
                wallSprite.setOrigin(wall.getHeight()/2,wall.getHeight()/2);
                wallSprite.setRotation(wall.getAngle());
                this.spriteArray.add(wallSprite);
                //endwall (сразу ставлю в место, где оно должно быть и там кручу вокруг своей оси)
                wallSprite = new Sprite(AssetLoader.wallEdge);
                wallSprite.setBounds(wall.getEndWallPoint().x-(wall.getHeight()/2),wall.getEndWallPoint().y-(wall.getHeight()/2), wall.getHeight(), wall.getHeight());
                wallSprite.setOrigin((wall.getHeight()/2),wall.getHeight()/2);
                wallSprite.setRotation(wall.getAngle());
                this.spriteArray.add(wallSprite);
            }
            numberOfWall+=1;
        }
    }

    public Array<Walls> getWallsArray() {
        return this.walls;
    }

    public Array<PolygonSprite> getPolySpriteArray() {
        return this.polySpriteArray;
    }

    public Array<Sprite> getSpriteArray(){
        return this.spriteArray;
    }
}
