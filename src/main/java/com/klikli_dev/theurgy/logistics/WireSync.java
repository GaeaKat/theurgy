package com.klikli_dev.theurgy.logistics;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.klikli_dev.theurgy.network.Networking;
import com.klikli_dev.theurgy.network.messages.MessageAddWires;
import com.klikli_dev.theurgy.network.messages.MessageRemoveWires;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

import java.util.Set;
import java.util.UUID;

public class WireSync {
    private static final WireSync instance = new WireSync();

    /**
     * one player can watch multiple chunks, and one chunk can be watched by multiple players, so we need a multimap
     * we do not need to keep track of the level, because if a player changes level he will unwatch all chunkPos in that level
     */
    private final SetMultimap<UUID, ChunkPos> playerToWatchedChunk = HashMultimap.create();
    /**
     * Reverse map to allow lookup of all players watching a chunk
     */
    private final SetMultimap<ChunkPos, UUID> watchedChunkToPlayers = HashMultimap.create();

    public static WireSync get() {
        return instance;
    }

    //TODO: send updates to wires to all watching players
    

    private void sendAddWiresInChunk(ServerPlayer player, ChunkPos chunkPos) {
        var wires = Wires.get(player.level());
        Networking.sendTo(player, new MessageAddWires(wires.getWires(chunkPos)));
    }

    private void sendRemoveWiresInChunk(ServerPlayer player, ChunkPos chunkPos) {
        //tricky, we now have to make sure not to remove any wires that are still in other watched chunks
        //luckily we have the reverse map, so we can check if a wire is still in there
        var manager = Wires.get(player.level());
        var wires = manager.getWires(chunkPos);
        var wiresToRemove = new ObjectOpenHashSet<Wire>();
        for (var wire : wires) {
            var otherChunksForWire = manager.getChunks(wire);

            //if the wire is only in one chunk we can safely remove it from the client
            if (otherChunksForWire.size() == 1) {
                wiresToRemove.add(wire);
                continue;
            }

            //if not we need to check if it in any of the other watched chunks of the player
            //if it is, we cannot remove it
            boolean isStillWatched = false;
            for (var otherChunk : otherChunksForWire) {
                //the current chunk has already been removed from playerToWatchedChunk before this method was called
                //so this will not lead to a "false positive" and we can safely just query the map.
                if (this.playerToWatchedChunk.containsEntry(player.getUUID(), otherChunk)) {
                    isStillWatched = true;
                    break;
                }
            }

            if (!isStillWatched) {
                wiresToRemove.add(wire);
            }
        }

        Networking.sendTo(player, new MessageRemoveWires(wiresToRemove));
    }

    public void onChunkWatch(ChunkWatchEvent.Watch event) {
        //TODO: probably save to do this async

        this.playerToWatchedChunk.put(event.getPlayer().getUUID(), event.getPos());
        this.watchedChunkToPlayers.put(event.getPos(), event.getPlayer().getUUID());
        this.sendAddWiresInChunk(event.getPlayer(), event.getPos());

    }

    public void onChunkUnWatch(ChunkWatchEvent.UnWatch event) {
        //TODO: probably save to do this async

        this.playerToWatchedChunk.remove(event.getPlayer().getUUID(), event.getPos());
        this.watchedChunkToPlayers.remove(event.getPos(), event.getPlayer().getUUID());
        this.sendRemoveWiresInChunk(event.getPlayer(), event.getPos());
    }
}
