package Murdak_v8;

import aic2022.user.*;

public class Knight extends CombatUnit {

    public Knight(UnitController _uc) {
        this.uc = _uc;
        this.data = new Data(uc);
        this.tools = new Tools(uc, data);
        this.movement = new Movement(uc, data);
    }

    void run() {

        while (true) {

            data.update();

            //BFS_Direction();

            report();

            attack();

            move();

            abilityOne();

            attack();

            getShrine();

            getChest();

            useArtifact();

            levelUp();

            //enterDungeon();

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
        uc.writeOnSharedArray(data.knightReportCh, uc.readOnSharedArray(data.knightReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.knightResetCh, 0);
    }

    void levelUp(){
        if(!data.enemyFound || uc.getInfo().getLevel() >= 2) return;
        if(uc.senseUnits(50,data.allyTeam,true).length > 0) return;
        if(uc.canLevelUp() && uc.getReputation() > data.knightLvlThreshold ) uc.levelUp();
    }

    void abilityOne(){

        if(uc.getInfo().getLevel() < 2) return;
        UnitInfo[] units = uc.senseUnits(50,data.allyTeam, true);

        for (UnitInfo u : units){
            if ( (u.getType() == UnitType.RANGER || u.getType() == UnitType.MAGE)
                 && !uc.isObstructed(uc.getLocation(), u.getLocation() ) ){

                Direction dir = uc.getLocation().directionTo(u.getLocation());
                Location loc = uc.getLocation().add(dir);
                if(data.enemyBaseFound && loc.distanceSquared(data.enemyBaseLoc) < 50) continue;
                if( uc.canUseFirstAbility(loc) ) uc.useFirstAbility(loc);

                return;

            }

        }

    }

}