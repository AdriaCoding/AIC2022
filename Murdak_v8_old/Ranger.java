package Murdak_v8_old;

import aic2022.user.UnitController;
import aic2022.user.UnitInfo;
import aic2022.user.UnitType;

public class Ranger extends CombatUnit {

    public Ranger (UnitController _uc) {
        this.uc = _uc;
        this.data = new Data(uc);
        this.tools = new Tools(uc, data);
        this.movement = new Movement(uc, data);
    }

    void run() {

        while (true) {

            data.update();

            report();

            attack();

            abilityOne();

            move();

            attack();

            getShrine();

            getChest();

            useArtifact();

            if (data.enemyBaseFound){
                tools.baseBFS(tools.encodeLoc(data.enemyBaseLoc), data.enemyBaseBFSCh);
            }

            uc.yield();
        }

    }

    @Override
    void move(){
        if(movement.doMicro() ) return;
        if(reinforce() )        return;
        if(accumulate() )       return;
        movement.explore();
    }

    @Override
    void reportMyself() {
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.unitResetCh, 0);
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.rangerReportCh, uc.readOnSharedArray(data.rangerReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.rangerResetCh, 0);
    }

    void abilityOne(){

        UnitInfo[] units = uc.senseUnits(36,data.allyTeam, true);

        for (UnitInfo u : units){
            if (u.getType() == UnitType.BARBARIAN || (u.getType() == UnitType.KNIGHT && u.getLevel() < 2) ){
                if(uc.getInfo().getLevel() < 2){
                    if(uc.canLevelUp() && uc.getReputation() > data.rangerLvlThreshold ) uc.levelUp();
                    return;
                }
                else if (uc.canUseFirstAbility( u.getLocation() ) ){
                    uc.useFirstAbility(u.getLocation());
                    return;
                }

            }

        }

    }






}
