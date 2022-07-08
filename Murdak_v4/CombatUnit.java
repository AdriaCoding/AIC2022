package Murdak_v4;

import aic2022.user.*;

public class CombatUnit extends Unit {

    Movement movement;

    void report() {
        reportMyself();
        reportEnemies();
        //reportEnvironment();
    }

    // placeholder, this is overridden for each different unit
    void reportMyself() {
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.unitResetCh, 0);
    }

    // Should be used by explorers

    void getChest(){
        ChestInfo[] chest = uc.senseChests(2);
        if (chest.length > 0){
            Direction dir = uc.getLocation().directionTo(chest[0].getLocation() );
            if(uc.canOpenChest(dir)) uc.openChest(dir);
        }
    }

    void move(){
        if(movement.doMicro() ) return;
        if(reinforce() )        return;
        if(accumulate() )       return;
        movement.explore();
    }

    boolean accumulate(){
        Location accumulationTarget = tools.decodeLoc(uc.readOnSharedArray(data.accumulationCh));
        if (uc.getRound() < data.accumulationRound){
            movement.moveTo(accumulationTarget);
            return true;
        }
        return false;
    }

    boolean reinforce(){
        if(!data.enemyFound) return false;
        Location loc = data.enemyLoc;

        if(uc.getLocation().distanceSquared(loc) < data.reinforcementDist && uc. getRound() > data.reinforcementRound){
            movement.moveTo(loc);
            return true;
        }
        return false;
    }

    void useArtifact(){

        ArtifactInfo[] stuff = uc.getArtifacts();
        if (stuff.length > 0 && uc.canUseArtifact(0) ){
            uc.useArtifact(0);
        }

    }

    void enterDungeon(){
        if (uc.senseTileTypeAtLocation(uc.getLocation()) == TileType.DUNGEON) return;
        for (Location entrance : uc.senseVisibleTiles(TileType.DUNGEON_ENTRANCE)){
            Direction dir = tools.isAdjacent(entrance, uc.getLocation());
            if(dir != Direction.ZERO){
                if(uc.canEnterDungeon(dir,dir)) uc.enterDungeon(dir,dir);
            }
        }
    }

}