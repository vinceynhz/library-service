package app.tandv.services.data;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author Vic on 9/5/2018
 **/
@Component
public class ChangeId {
    public class ChangeUpdate {
        private String before;
        private String after;

        public String getBefore() {
            return before;
        }

        public void setBefore(String before) {
            this.before = before;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }
    }

    private String changeId;

    public ChangeId() {
        changeId = UUID.randomUUID().toString();
    }

    public synchronized ChangeUpdate update() {
        ChangeUpdate changeUpdate = new ChangeUpdate();
        changeUpdate.before = changeId;
        changeId = UUID.randomUUID().toString();
        changeUpdate.after = changeId;
        return changeUpdate;
    }

    public String getChangeId() {
        return changeId;
    }
}
