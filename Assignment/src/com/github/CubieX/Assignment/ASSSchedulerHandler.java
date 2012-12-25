package com.github.CubieX.Assignment;

public class ASSSchedulerHandler
{

    private Assignment aInst = null;

    public ASSSchedulerHandler(Assignment aInst)
    {
        this.aInst = aInst;
    }

    public void startCleanupScheduler_SyncRep()
    {      
        aInst.getServer().getScheduler().runTaskTimer(aInst, new Runnable()
        {
            public void run()
            {
                aInst.cleanupAssignmentsInDB(null);
            }
        }, (20*600L), 20*10800L); // 10 min initial delay, 3 hour period        
    }
}
