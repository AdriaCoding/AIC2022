package Murdak_v4;

import aic2022.user.ChestInfo;
import aic2022.user.Location;
import aic2022.user.TileType;
import aic2022.user.UnitController;

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

            attack();

            move();

            attack();

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

    @Override
    void move(){

        if(movement.doMicro() ) return;
        if(seekChest() )        return;
        if(accumulate() )       return;
        movement.explore();

    }
    @Override
    boolean accumulate(){
        Location accumulationTarget = tools.decodeLoc(uc.readOnSharedArray(data.accumulationCh));
        if (uc.getRound() < data.scoutAccumulationRound){
            movement.moveTo(accumulationTarget);
            return true;
        }
        return false;
    }

    boolean seekChest(){
        ChestInfo[] chests = uc.senseChests(data.seekChestDist);
        for (ChestInfo chest : chests) {
            if (!uc.isObstructed(chest.getLocation(), uc.getLocation())) {
                movement.moveTo(chest.getLocation());
                return true;
            }
        }
        return false;
    }


}


