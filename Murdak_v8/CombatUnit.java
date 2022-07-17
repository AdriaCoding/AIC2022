package Murdak_v8;

import aic2022.user.*;

public class CombatUnit extends Unit {

    Movement movement;

    void report() {
        reportMyself();
        reportEnemies();
        if(!uc.getType().equals(UnitType.BASE) && !uc.senseTileTypeAtLocation(uc.getLocation()).equals(TileType.DUNGEON)) {
            int dircode = uc.readOnSharedArray(tools.encodeLoc(uc.getLocation())) / 10;
            if (dircode > 0 && dircode < 9) {
                        uc.setOrientation(tools.dirsBFS[dircode - 1].opposite());
            }
        }
        //reportEnvironment();
    }

    // placeholder, this is overridden for each different unit
    void reportMyself() {
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.unitResetCh, 0);
    }

    void getChest(){
        ChestInfo[] chest = uc.senseChests(2);
        if (chest.length > 0){
            Direction dir = uc.getLocation().directionTo(chest[0].getLocation() );
            if(uc.canOpenChest(dir)) uc.openChest(dir);
        }
    }

    void move(){
        if(movement.doMicro() ) return;
        if(reinforceBase() )    return;
        if(reinforce() )        return;
        if(seekShrine() )       return; //TODO mirar potser de posar-ho abans de reinforce
        if(accumulate() )       return;
        movement.explore();
    }

    boolean reinforceBase(){

        Location accumulationTarget = tools.decodeLoc(uc.readOnSharedArray(data.accumulationCh));

        if(data.baseInDanger){
            movement.moveTo(accumulationTarget);
            return true;
        }
        return false;
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

    boolean seekShrine(){
        ShrineInfo[] shrines = uc.senseShrines(data.seekShrineDist);
        for (ShrineInfo shrine : shrines) {
            for (Direction dir : data.dirs) {
                if (dir.isEqual(Direction.ZERO) || !uc.canSenseLocation((shrine.getLocation().add(dir)) ) ) continue;
                if (!uc.isObstructed(shrine.getLocation().add(dir), uc.getLocation()) && shrine.getOwner() != data.allyTeam) {
                    movement.moveTo(shrine.getLocation().add(dir));
                    return true;
                }
            }
        }
        return false;
    }

}