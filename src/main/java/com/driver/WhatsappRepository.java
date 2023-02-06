package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below-mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }
    public String createUser(String name, String mobile) throws Exception {
        //If the mobile number exists in database, throw "User already exists" exception
        //Otherwise, create the user and return "SUCCESS"
        if(userMobile.contains(mobile)){
            throw new Exception("User already exists");
        }else{
            userMobile.add(mobile);
            User user =new User(name,mobile);
            return "SUCCESS";
        }
    }
    public Group createGroup(List<User> users){
        // The list contains at least 2 users where the first user is the admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group customGroupCount". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // If group is successfully created, return group.
        int sizeOfgroup=users.size();

        String groupName="";
        if(sizeOfgroup==2){
            groupName=users.get(1).getName();
        }else{
            this.customGroupCount++;
            groupName="Group "+this.customGroupCount;
        }
        Group group= new Group(groupName,sizeOfgroup);

        //managing admin
        adminMap.put(group,users.get(0));
        groupUserMap.put(group,users);
        groupMessageMap.put(group,new ArrayList<Message>());
        return group;
    }
    public int createMessage(String content){
        // The 'i^th' created message has message id 'i'.
        // Return the message id.
        this.messageId++;
        Message message = new Message(this.messageId,content);
        return message.getId();
    }
    public int sendMessage(Message message, User sender, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        //If the message is sent successfully, return the final number of messages in that group.

        if(!groupUserMap.containsKey(group)) throw new Exception("Group does not exist");
        else{
             List<User> users = groupUserMap.get(group);
             boolean isUserPresent=false;
             for(User itr: users){
                 if(itr.equals(sender)){
                     isUserPresent=true;
                     break;
                 }
             }
             if(!isUserPresent) throw new Exception("You are not allowed to send message");
             else {
                 groupMessageMap.get(group).add(message);
                 senderMap.put(message,sender);
                 return groupMessageMap.get(group).size();
             }
        }
    }
    public String changeAdmin(User approver, User user, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        //Throw "User is not a participant" if the user is not a part of the group
        //Change the admin of the group to "user" and return "SUCCESS".
        boolean isGroupExist=groupUserMap.containsKey(group);
        if(!isGroupExist) throw new Exception("Group does not exist");
        List<User> groupUsers=groupUserMap.get(group);
        if(groupUsers.get(0).equals(approver)) throw new Exception("Approver does not have rights");
        if(!groupUsers.contains(user)) throw new Exception("User is not a participant");
        //admin setup
//        groupUsers.remove(user);
//        groupUsers.set(0,user);
//        Collections.swap(groupUsers,0,groupUsers.indexOf(user));
        adminMap.put(group,user);
        return "SUCCESS";
    }
    public int removeUser(User user) throws Exception{
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
//        boolean isUserPresent=false;
//        Set<User> userSet= new HashSet<>();
//        for(List<User> itr: groupUserMap.values()){
//            userSet.addAll(itr);
//            for(User currentUser: itr){
//                if (currentUser.equals(user)) {
//                    isUserPresent = true;
//                    break;
//                }
//            }
//        }

        Group group=null;
        for(Group currentGroup:groupUserMap.keySet()){
            for(User currentUser:groupUserMap.get(currentGroup)){
                if (currentUser.equals(user)) {
//                    isUserPresent = true;
                    group=currentGroup;
                    break;
                }
            }
            if(group!=null) break;
        }
        if(group==null) throw new Exception("User not found");
        else{
            if(groupUserMap.get(group).get(0).equals(user)){
                throw new Exception("Cannot remove admin");
            }else{
                groupUserMap.get(group).remove(user);
                userMobile.remove(user.getName());

                //remove from hashmap with values
                Set<Message> messageSet = new HashSet<>();
                for(Message message :senderMap.keySet()){
                    if(senderMap.get(message).equals(user)) messageSet.add(message);
                }
                senderMap.keySet().removeAll(messageSet);

                Set<Message> gruopSet = new HashSet<>();
                for(Message message :messageSet){
                    groupMessageMap.get(group).remove(message);
                }
                senderMap.keySet().removeAll(messageSet);

                int updatedUsersInGroup=groupUserMap.get(group).size();
                int updatedMessagesInGroup=groupMessageMap.get(group).size();
                int totalMessage=senderMap.size();

                return updatedUsersInGroup+updatedMessagesInGroup+totalMessage;
            }
        }
    }

    public String findMessage(Date start, Date end, int K) throws Exception{
        // Find the Kth the latest message between start and end (excluding start and end)
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception
        List<Message> ls = new ArrayList<>();
        for(Message message:senderMap.keySet()){
            if(message.getTimestamp().after(start) && message.getTimestamp().before(end)){
                ls.add(message);
            }
        }
        if(ls.size()<K) throw new Exception("K is greater than the number of messages");

        Collections.sort(ls, new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return o2.getTimestamp().compareTo(o1.getTimestamp());
            }
        });

        return ls.get(K-1).getContent();
    }
}
