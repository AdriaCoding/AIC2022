package Murdak_v3;

import aic2022.user.*;

public class Explorer extends CombatUnit {
    public Explorer(UnitController _uc) {
        this.uc = _uc;
        this.data = new Data(uc);
        this.tools = new Tools(uc, data);
        this.movement = new Movement(uc, data);
    }

    void run() {

        while (true) {

            data.update();

            report();

            senseStuff();

            attackN();

            move();

            attack();

            attackN();

            getShrine();

            getChest();

            useArtifact();

            //enterDungeon();

            uc.yield();
        }

    }


    void senseStuff(){
        if (uc.senseTileTypeAtLocation(uc.getLocation()) == TileType.DUNGEON){

            for(ChestInfo chest : uc.senseChests()){
                data.saveChest(chest);
            }
        }
        else{
            for(Location loc : uc.getVisibleLocations()){
                TileType tileType = uc.senseTileTypeAtLocation(loc);
                if (tileType == TileType.SHRINE) data.saveShrine(loc);
                if (tileType == TileType.DUNGEON_ENTRANCE) data.saveDungeon(loc);
            }
        }

    }

}
