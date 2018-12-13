package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.simonit.libgdxsvg.SVG;

public class MyGdxGame extends ApplicationAdapter {
    SpriteBatch batch;
    Texture img;
    SVG svg;

    @Override
    public void create() {
        this.batch = new SpriteBatch();
        this.img = new Texture("badlogic.jpg");

        this.svg = new SVG("powered_by_libGDX.svg");
        svg.setWidth(svg.getWidth() * 8, true);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        this.batch.begin();
        this.batch.draw(this.img, 0, 0);
        this.svg.draw(this.batch, 0, 300);
        this.batch.end();
    }

    @Override
    public void dispose() {
        this.batch.dispose();
        this.img.dispose();
        this.svg.dispose();
    }
}
