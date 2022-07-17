package Murdak_v8;

import aic2022.user.UnitController;
import aic2022.user.UnitInfo;

public class Barbarian extends CombatUnit {

    public Barbarian (UnitController _uc) {
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

            //abilityOne();

            move();

            attack();

            //abilityOne();

            getShrine();

            getChest();

            useArtifact();

            //levelUp();

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
        uc.writeOnSharedArray(data.barbarianReportCh, uc.readOnSharedArray(data.barbarianReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.barbarianResetCh, 0);
    }


    void abilityOne(){

        UnitInfo[] enemies = uc.senseUnits(8,data.allyTeam, true);
        UnitInfo[] allies = uc.senseUnits(8,data.allyTeam);

        if(enemies.length > 3 && allies.length == 0) {
            if (uc.canUseFirstAbility(uc.getLocation())) uc.useFirstAbility(uc.getLocation());
        }

    }

    void levelUp(){
        if(!data.enemyFound || uc.getInfo().getLevel() >= 2) return;
        if(uc.senseUnits(32,data.allyTeam,true).length > 0) return;
        if (uc.canLevelUp() && uc.getReputation() > data.barbarianLvlThreshold) uc.levelUp();
    }

}