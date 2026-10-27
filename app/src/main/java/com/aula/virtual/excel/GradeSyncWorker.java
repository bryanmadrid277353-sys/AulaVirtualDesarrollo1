package com.aula.virtual.excel;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.local.AppDatabase;
import java.util.List;

public class GradeSyncWorker extends Worker {

    public GradeSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Subject> subjects = db.subjectDao().getAllSubjects();
            if (subjects != null) {
                for (Subject subject : subjects) {
                    Uri linkedUri = GradeSyncManager.getLinkedUri(context, subject.id);
                    if (linkedUri != null) {
                        GradeSyncManager.syncNowInternal(context, subject, linkedUri, null);
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
