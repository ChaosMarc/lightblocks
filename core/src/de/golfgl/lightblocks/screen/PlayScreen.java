package de.golfgl.lightblocks.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;

import de.golfgl.lightblocks.LightBlocksGame;
import de.golfgl.lightblocks.model.GameModel;
import de.golfgl.lightblocks.model.Gameboard;
import de.golfgl.lightblocks.model.IGameModelListener;
import de.golfgl.lightblocks.model.Tetromino;
import de.golfgl.lightblocks.scenes.BlockActor;
import de.golfgl.lightblocks.scenes.BlockGroup;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

/**
 * The main playing screen
 * <p>
 * Übernimmt auch den Adapter zwischen GameModel und Input/GUI
 * <p>
 * Created by Benjamin Schulte on 16.01.2017.
 */

public class PlayScreen extends AbstractScreen implements IGameModelListener {

    private final BlockGroup blockGroup;
    private final BlockActor[][] blockMatrix;
    private final BlockActor[] nextTetro;
    public GameModel gameModel;
    PlayScreenInput inputAdapter;
    Music music;
    float lastAccX = 0;

    private boolean isPaused = true;

    public PlayScreen(LightBlocksGame app, PlayScreenInput inputAdapter) {
        super(app);

        blockMatrix = new BlockActor[Gameboard.GAMEBOARD_COLUMNS][Gameboard.GAMEBOARD_ROWS];
        nextTetro = new BlockActor[Tetromino.TETROMINO_BLOCKCOUNT];

        inputAdapter.setPlayScreen(this);
        this.inputAdapter = inputAdapter;

        // Die Blockgroup nimmt die Steinanimation auf
        blockGroup = new BlockGroup();
        blockGroup.setTransform(false);

        // 10 Steine breit, 20 Steine hoch
        blockGroup.setX((LightBlocksGame.nativeGameWidth - Gameboard.GAMEBOARD_COLUMNS * BlockActor.blockWidth) / 4);
        blockGroup.setY((LightBlocksGame.nativeGameHeight - (Gameboard.GAMEBOARD_ROWS - 1) * BlockActor.blockWidth) /
                2);

        // mit Quatsch initialisieren
//        for (int i = 0; i < 10; i++) {
//            for (int j = 0; j < 20; j++) {
//                BlockActor block = new BlockActor(app);
//                block.setX(i * BlockActor.blockWidth);
//                block.setY(j * BlockActor.blockWidth);
//                block.setEnlightened(MathUtils.random(0, 1) == 0);
//                blockGroup.addActor(block);
//            }
//        }

        stage.addActor(blockGroup);

        stage.getRoot().setColor(Color.CLEAR);

        // Game Model erst hinzufügen, wenn die blockgroup schon steht
        gameModel = new GameModel(this);

        if (app.savegame.hasSavedGame())
            gameModel.loadGameModel(app.savegame.loadGame());
        else
            gameModel.startNewGame();

    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 30f);

        if (!isPaused)
            gameModel.update(delta);

        super.render(delta);
    }

    @Override
    public void show() {
        Gdx.input.setCatchBackKey(true);
        Gdx.input.setInputProcessor(inputAdapter);
        stage.getRoot().addAction(Actions.fadeIn(1));
    }

    public void goBackToMenu() {
        if (!gameModel.isGameOver())
            app.savegame.saveGame(gameModel.saveGameModel());

        app.setScreen(app.mainMenuScreen);
        if (music != null)
            music.dispose();

        stage.dispose();
    }

    @Override
    public void pause() {
        super.pause();

        if (!isPaused)
            switchPause(true);
    }

    public void switchPause(boolean immediately) {

        if (gameModel.isGameOver())
            return;

        isPaused = !isPaused;

        final float fadingInterval = immediately ? 0 : .2f;

        //inform input adapter, too
        inputAdapter.isPaused = isPaused;

        blockGroup.clearActions();

        if (!isPaused) {

            if (music != null)
                music.play();

            if (blockGroup.getColor().a < 1) {
                blockGroup.addAction(Actions.fadeIn(fadingInterval));
                gameModel.setFreezeInterval(fadingInterval);
            }

            //inform the game model that there was a pause
            gameModel.fromPause();
        } else {
            blockGroup.addAction(Actions.fadeOut(fadingInterval));
            if (music != null)
                music.pause();

            // Spielstand speichern
            app.savegame.saveGame(gameModel.saveGameModel());
        }
    }

    @Override
    public void insertNewBlock(int x, int y) {
        BlockActor block = new BlockActor(app);
        insertBlock(x, y, block);
    }

    private void insertBlock(int x, int y, BlockActor block) {
        block.setX(x * BlockActor.blockWidth);
        block.setY(y * BlockActor.blockWidth);
        blockGroup.addActor(block);
        blockMatrix[x][y] = block;
    }

    @Override
    public void moveTetro(Integer[][] v, int dx, int dy) {
        if (dx != 0 || dy != 0) {
            // erst alle vom Spielbrett einsammeln...
            Array<BlockActor> blocks = removeBlockActorsFromMatrix(v);

            //... und dann neu ablegen
            //TODO die geschwindigkeit muss genauso hoch sein wie beim SOFTDROP!
            for (int i = 0; i < v.length; i++) {
                BlockActor block = blocks.get(i);
                int x = v[i][0];
                int y = v[i][1];
                block.setMoveAction(Actions.moveTo((x + dx) * BlockActor.blockWidth, (y + dy) * BlockActor
                        .blockWidth, 1 / 30f));
                blockMatrix[x + dx][y + dy] = block;
            }
        }
    }

    private Array<BlockActor> removeBlockActorsFromMatrix(Integer[][] v) {
        Array<BlockActor> blocks = new Array<BlockActor>(v.length);

        for (Integer[] xy : v) {
            if (blockMatrix[xy[0]][xy[1]] == null)
                Gdx.app.error("BLOCKS", "Block null at " + xy[0].toString() + " " + xy[1].toString());

            blocks.add(blockMatrix[xy[0]][xy[1]]);
            blockMatrix[xy[0]][xy[1]] = null;
        }
        return blocks;
    }

    @Override
    public void rotateTetro(Integer[][] vOld, Integer[][] vNew) {
        app.rotateSound.play();

        // erst alle vom Spielbrett einsammeln...
        Array<BlockActor> blocks = removeBlockActorsFromMatrix(vOld);

        //... und dann neu ablegen
        for (int i = 0; i < vOld.length; i++) {
            BlockActor block = blocks.get(i);
            int newx = vNew[i][0];
            int newy = vNew[i][1];
            block.setMoveAction(Actions.moveTo((newx) * BlockActor.blockWidth, (newy) * BlockActor.blockWidth, 1 /
                    10f));
            blockMatrix[newx][newy] = block;
        }


    }

    @Override
    public void clearLines(IntArray linesToRemove) {

        final float removeDelayTime = .3f;
        final float removeFadeOutTime = .2f;
        final float moveActorsTime = .1f;

        gameModel.setFreezeInterval(removeDelayTime);

        // Vorbereitung zum Heraussuchen der Zeilen, die welche ersetzen
        IntArray lineMove = new IntArray(Gameboard.GAMEBOARD_ROWS);
        for (int i = 0; i < Gameboard.GAMEBOARD_ROWS; i++)
            lineMove.add(i);


        for (int i = linesToRemove.size - 1; i >= 0; i--) {
            int y = linesToRemove.get(i);

            // die zu entfernende Zeile durchgehen und alle Blöcke erleuchten
            // und entfernen
            for (int x = 0; x < Gameboard.GAMEBOARD_COLUMNS; x++) {
                BlockActor block = blockMatrix[x][y];
                blockMatrix[x][y] = null;
                block.setEnlightened(true);

                // die untersten zusammenhängenden Zeilen rausschieben
                if (y == i)
                    block.setMoveAction(sequence(Actions.delay(removeDelayTime), Actions.moveBy(0, -(i + 1) *
                            BlockActor.blockWidth, moveActorsTime)));

                app.removeSound.play();
                block.addAction(sequence(Actions.delay(removeDelayTime), Actions.fadeOut(removeFadeOutTime),
                        Actions.removeActor()));
            }

            // heraussuchen durch weile Zeile diese hier ersetzt wird
            for (int higherY = y; higherY < Gameboard.GAMEBOARD_ROWS; higherY++)
                if (higherY < Gameboard.GAMEBOARD_ROWS - 1)
                    lineMove.set(higherY, lineMove.get(higherY + 1));
                else
                    lineMove.set(higherY, -1);

        }

        for (int i = 0; i < lineMove.size; i++) {
            for (int x = 0; x < Gameboard.GAMEBOARD_COLUMNS; x++) {

                if (lineMove.get(i) >= 0) {
                    BlockActor block = blockMatrix[x][lineMove.get(i)];
                    blockMatrix[x][lineMove.get(i)] = null;
                    blockMatrix[x][i] = block;
                    if (block != null)
                        block.setMoveAction(sequence(Actions.delay(removeDelayTime), (Actions.moveTo((x) *
                                BlockActor.blockWidth, (i) * BlockActor.blockWidth, moveActorsTime))));
                }

            }
        }


    }

    @Override
    public void setGameOver(boolean b) {
        if (music != null)
            music.stop();
        app.savegame.resetGame();
    }

    @Override
    public void showNextTetro(Integer[][] relativeBlockPositions) {
        // ein neuer nächster-Stein wurde bestimmt. Wir zeigen ihn einfach über dem Spielfeld an
        // Er wird aber zunächst nicht der Blockgroup hinzugefügt, damit er wenn er einfliegt keine Steine auf dem
        // Spielfeld überlagert

        final float offsetX = LightBlocksGame.nativeGameWidth - (Tetromino.TETROMINO_BLOCKCOUNT + .5f) * BlockActor
                .blockWidth;
        final float offsetY = (Gameboard.GAMEBOARD_ROWS - 3.5f) * BlockActor.blockWidth;

        for (int i = 0; i < Tetromino.TETROMINO_BLOCKCOUNT; i++) {
            nextTetro[i] = new BlockActor(app);
            nextTetro[i].setPosition((i == 0 || i == 2) ? -BlockActor.blockWidth : LightBlocksGame.nativeGameWidth +
                            BlockActor.blockWidth,
                    (i >= 2) ? 0 : LightBlocksGame.nativeGameHeight);
            nextTetro[i].setMoveAction(Actions.moveTo(offsetX + relativeBlockPositions[i][0] * BlockActor.blockWidth,
                    offsetY + relativeBlockPositions[i][1] * BlockActor.blockWidth, .5f, Interpolation.fade));
            nextTetro[i].addAction(Actions.alpha(.5f, .5f, Interpolation.fade));
            nextTetro[i].getColor().a = 0;

            blockGroup.addActorAt(0, nextTetro[i]);
        }
    }

    @Override
    public void activateNextTetro(Integer[][] boardBlockPositions) {

        for (int i = 0; i < Tetromino.TETROMINO_BLOCKCOUNT; i++) {
            // den bereits in nextTetro instantiierten Block ins Spielfeld an die gewünschte Stelle bringen
            BlockActor block = nextTetro[i];

            final int x = boardBlockPositions[i][0];
            final int y = boardBlockPositions[i][1];

            if (block == null) {
                //beim Spielstart noch nicht gesetzt und die Animation macht auch keinen Sinn,
                //dann gleich an Zielposition instanziieren
                block = new BlockActor(app);
                insertBlock(x, y, block);
            } else {
                nextTetro[i] = null;
                blockMatrix[x][y] = block;
                block.addAction(Actions.fadeIn(.1f));
                block.setMoveAction(Actions.moveTo(x * BlockActor.blockWidth, y * BlockActor.blockWidth, .1f,
                        Interpolation.fade));
            }
            block.setEnlightened(true);
        }
    }

    @Override
    public void pinTetromino(Integer[][] currentBlockPositions) {
        app.dropSound.play();
        for (Integer[] vAfterMove : currentBlockPositions)
            blockMatrix[vAfterMove[0]][vAfterMove[1]].setEnlightened(false);
    }

    public void setMusic(boolean playMusic) {
        if (playMusic) {
            music = Gdx.audio.newMusic(Gdx.files.internal("sound/dd.ogg"));
            music.setVolume(1f);                 // sets the volume to half the maximum volume
            music.setLooping(true);
        } else if (music != null)
            music.dispose();

    }
}
