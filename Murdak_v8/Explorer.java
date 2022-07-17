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

            enterDungeon();

            evaluateDungeon();

            attack();

            move();

            attack();

            getShrine();

            getChest();

            giveArtifact();

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
        if(!data.inDungeon && movement.doMicro())   return;
        if(!data.inDungeon && seekChest())          return;
        if(seekDungeon() )                          return;
        if(data.inDungeon && seekChest())           return;
        if(data.inDungeon && movement.doMicro() )   return;
        if(seekShrine() )                           return;
        movement.explore();

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
        if (tile == TileType.DUNGEON_ENTRANCE) return false;

        //Conditions to NOT enter a dungeon
        if (uc.getRound()%400 > 320 && !inDungeon() ) return false;
        if (data.escapeDungeon      && !inDungeon() ) return false;

        //Conditions to NOT exit a dungeon
        if ( (uc.getRound()%400 < 320 && inDungeon() ) && !data.escapeDungeon) return false;


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

        if(!inDungeon() ) return;

        int dungeonChestDist = 36;
        int dungeonEnemyDist = 64;

        float dungeonDanger = 0;

        ChestInfo[] chests = uc.senseChests(dungeonChestDist);
        for (ChestInfo chest : chests) {
            if (!uc.isObstructed(chest.getLocation(), uc.getLocation())) {
                float d = uc.getLocation().distanceSquared(chest.getLocation() );
                dungeonDanger += (float) chest.getGold()/( 10 + d/4 );
            }
        }

        UnitInfo[] enemies = uc.senseUnits(dungeonChestDist,data.allyTeam,true);
        for (UnitInfo enemy : enemies) {
            if (!uc.isObstructed(enemy.getLocation(), uc.getLocation())) {
                dungeonDanger -= (float) enemy.getType().getStat(UnitStat.ATTACK)/5;
            }
        }

        //uc.println("DungeonDanger in dungeon"+uc.getLocation() +" is " + dungeonDanger);

        if(dungeonDanger <= 0){
            data.escapeDungeon = true;
        }

    }

    void giveArtifact(){
        ArtifactInfo[] artifacts = uc.getArtifacts();
        for (ArtifactInfo artifact : artifacts){
            UnitInfo[] units = uc.senseUnits(2,data.allyTeam);
            for(UnitInfo unit : units) {
                if (uc.canGiveArtifact(0, uc.getLocation().directionTo(unit.getLocation() ) ) ){
                    uc.giveArtifact(0, uc.getLocation().directionTo(unit.getLocation() ) );
                }
            }
        }
    }

    void enterDungeon(){
        if(uc.getRound() < data.dungeonExplorationRound) return;

        TileType tile = uc.senseTileTypeAtLocation(uc.getLocation());
        if (tile == TileType.DUNGEON_ENTRANCE) return;

        //Conditions to NOT enter a dungeon
        if (uc.getRound()%400 > 320 && !inDungeon() ) return;
        if (data.escapeDungeon      && !inDungeon() ) return;

        //Conditions to NOT exit a dungeon
        if ( (uc.getRound()%400 < 320 && inDungeon() ) && !data.escapeDungeon) return;


        Location[] dungeons = uc.senseVisibleTiles(2,TileType.DUNGEON_ENTRANCE);
        for (Location entrance : dungeons) {

            Direction d1 = uc.getLocation().directionTo(entrance);
            for (Direction d2 : data.dirs) {
                if (!d2.isEqual(Direction.ZERO) && uc.canEnterDungeon(d1, d2)) {
                    uc.enterDungeon(d1, d2);
                    return;
                }
            }
        }
    }

    boolean inDungeon(){
        return (uc.senseVisibleTiles(TileType.DUNGEON).length > 0);

    }

}


