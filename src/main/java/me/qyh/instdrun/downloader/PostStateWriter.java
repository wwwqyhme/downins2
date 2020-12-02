package me.qyh.instdrun.downloader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class PostStateWriter extends DWriter<PostState> {

    public PostStateWriter(Path file) {
        super(file);
    }

    public synchronized void setComplete(String postId) {
        super.consume(states->{
            for(PostState state : states) {
                if(state.getPostId().equals(postId)){
                    state.setComplete(true);
                    break;
                }
            }
            return true;
        });
    }

    @Override
    protected Class<PostState> getType() {
        return PostState.class;
    }

    public static void main(String [] args){
        PostStateWriter writer = new PostStateWriter(Paths.get("d:/downins/qyhqym/posts.txt"));
        List<PostState> uncompleted = new ArrayList<>();
        writer.consume(states->{
            uncompleted.addAll(states.stream().filter(Predicate.not(PostState::isComplete)).collect(Collectors.toList()));
            return false;
        });
        for(int i=0;i<46;i++){
            int finalI = i;
            writer.consume(states->{
                states.get(finalI).setComplete(true);
                return true;
            });
        }
        writer.consume(states->{

            return true;
        });
    }
}
