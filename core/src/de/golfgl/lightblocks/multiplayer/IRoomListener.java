package de.golfgl.lightblocks.multiplayer;

/**
 * To this listener multiplayer rooms send their messages
 * <p>
 * IMPORTANT: All methods are called on the render thread. The caller is reponsible, not the listener implementing
 * class!
 * <p>
 * <p>
 * Created by Benjamin Schulte on 26.02.2017.
 */

public interface IRoomListener {

    /**
     * called when a room was joined or left
     */
    public void multiPlayerRoomStateChanged(AbstractMultiplayerRoom.RoomState roomState);

    /**
     * called when inhabitants of the room changed.
     *
     * @param mpo PlayerChanged object with further information
     */
    public void multiPlayerRoomInhabitantsChanged(MultiPlayerObjects.PlayerChanged mpo);

    public void multiPlayerGotErrorMessage(Object o);
}
