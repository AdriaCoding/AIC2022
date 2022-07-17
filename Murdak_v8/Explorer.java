package Murdak_v8;

import aic2022.user.*;

public class Explorer extends CombatUnit {
    public Explorer(UnitController _uc) {
        this.uc = _uc;
        this.data = new Data(uc);
        this.tools = new Tools(uc, data);
        this.movement = new Movement(uc, data);
    }
    int cd = -1;        // the dungeon is currently being considered
    void run() {

        while (true) {

            data.update();

            report();

            evaluateDungeon(cd);

            attack();

            move();

            attack();

            getShrine();

            getChest();

            useArtifact();

            senseStuff();

            if (cd >= 0 && uc.getRound() > 20){      //cd == -1 if there is no dungeon
                int targetLocCode = uc.readOnSharedArray(data.dungeonCh[cd])%100000;
                if (targetLocCode != 0) tools.dungeonBFS(targetLocCode, cd);
            }
            uc.yield();
        }

    }

    @Override
    void reportMyself() {
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.unitResetCh, 0);
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.scoutReportCh, uc.readOnSharedArray(data.scoutReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.scoutResetCh, 0);
    }

    @Override
    void move(){

        if(seekDungeon() )      return;
        if(seekChest() )        return;
        if(movement.doMicro() ) return; //TEST maybe it needs to go first still
        if(seekShrine() )       return;
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

    boolean seekDungeon(){

        if(uc.getRound() < data.dungeonExplorationRound) return false;

        TileType tile = uc.senseTileTypeAtLocation(uc.getLocation());
        boolean isInDungeon = tile.equals(TileType.DUNGEON);

        if (tile == TileType.DUNGEON_ENTRANCE) return false;

        //Conditions to NOT enter a dungeon
        if (uc.getRound()%400 > 320 && tile != TileType.DUNGEON) return false;
        if (data.escapeDungeon      && tile != TileType.DUNGEON) return false;

        //Conditions to NOT exit a dungeon
        if ( (uc.getRound()%400 < 320 && tile == TileType.DUNGEON) && !data.escapeDungeon) return false;


        Location[] dungeons = uc.senseVisibleTiles(TileType.DUNGEON_ENTRANCE);
        for (Location entrance : dungeons) {
            if (!isInDungeon) {
                if (uc.readOnSharedArray(tools.encodeLoc(entrance))%10 != 8){
                    cd = data.saveDungeon(entrance);
                }
            }

            if(uc.getLocation().distanceSquared(entrance) <= 2){
                Direction d1 = uc.getLocation().directionTo(entrance);
                for (Direction d2 : data.dirs) {
                    if (!d2.isEqual(Direction.ZERO) && uc.canEnterDungeon(d1, d2)) {
                        uc.enterDungeon(d1, d2);
                        if (isInDungeon){
                            Location dungeonExit = uc.getLocation().add(d2.opposite());
                            uc.writeOnSharedArray(tools.encodeLoc(dungeonExit),
                                    8 + tools.encodeLoc(entrance)*10);
                            // 8 is a special TileType code for dungeon exits.
                        }
                        return true;
                    }
                }
            }

            if (!uc.isObstructed(entrance, uc.getLocation())) {
                movement.moveTo(entrance);
                return true;
            }
        }
        return false;
    }

    void evaluateDungeon(int di){   //di stands for dungeon index

        if(uc.senseTileTypeAtLocation(uc.getLocation()) != TileType.DUNGEON) return;

        float dungeonDanger = 0;

        ChestInfo[] chests = uc.senseChests(data.seekChestDist);
        for (ChestInfo chest : chests) {
            if (!uc.isObstructed(chest.getLocation(), uc.getLocation())) {
                dungeonDanger += (float) chest.getGold()/10;
            }
        }

        UnitInfo[] enemies = uc.senseUnits(data.seekChestDist,data.allyTeam,true);
        for (UnitInfo enemy : enemies) {
            if (!uc.isObstructed(enemy.getLocation(), uc.getLocation())) {
                dungeonDanger -= (float) enemy.getType().getStat(UnitStat.ATTACK)/5;
            }
        }

        if(dungeonDanger <= 0){
            data.escapeDungeon = true;
        }

    }

}


