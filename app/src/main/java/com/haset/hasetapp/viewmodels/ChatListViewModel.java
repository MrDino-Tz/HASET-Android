package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.haset.hasetapp.models.Conversation;
import com.haset.hasetapp.repositories.ChatRepository;

import java.util.List;

public class ChatListViewModel extends AndroidViewModel {
    private final ChatRepository repository;
    private LiveData<List<Conversation>> conversations;

    public ChatListViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ChatRepository();
    }

    public LiveData<List<Conversation>> getConversations(String userId) {
        if (conversations == null) {
            conversations = repository.getConversations(userId);
        }
        return conversations;
    }

    public void syncUnreadCounts(String userId, List<Conversation> conversations, Runnable onCountUpdated) {
        repository.syncUnreadCounts(getApplication(), userId, conversations, onCountUpdated);
    }

    public void deleteConversation(String userId, String otherUserId) {
        repository.deleteConversation(userId, otherUserId);
    }

    public void toggleArchive(String userId, String otherUserId, boolean isArchived) {
        repository.toggleArchiveConversation(userId, otherUserId, isArchived);
    }
}
