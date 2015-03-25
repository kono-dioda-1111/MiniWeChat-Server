package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import model.HibernateSessionFactory;
import model.User;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.google.protobuf.InvalidProtocolBufferException;


import protocol.ProtoHead;
import protocol.Data.UserData;
import protocol.Msg.AddFriendMsg;
import protocol.Msg.DeleteFriendMsg;
import protocol.Msg.GetUserInfoMsg;


/**
 * 主服务器下的子服务器 负责通讯录相关事件
 * 
 * @author wangfei
 *
 */
public class Server_Friend {
	public static Server_Friend instance = new Server_Friend();
	
	private Server_Friend(){
		
	}
	
	/**
	 * 搜索用户
	 * @param networkMessage
	 * @author wangfei
	 * @time 2015-03-23
	 */
	public void getUserInfo(NetworkMessage networkMessage){
		try {
			GetUserInfoMsg.GetUserInfoReq getUserInfoObject =GetUserInfoMsg.GetUserInfoReq.parseFrom(networkMessage.getMessageObjectBytes());
			GetUserInfoMsg.GetUserInfoRsp.Builder getUserInfoBuilder = GetUserInfoMsg.GetUserInfoRsp.newBuilder();
			
			Session session = HibernateSessionFactory.getSession();
			Criteria criteria = session.createCriteria(User.class);
			criteria.add(Restrictions.eq("userId", getUserInfoObject.getTargetUserId()));
			if(criteria.list().size()>0){
				//不支持模糊搜索 所以如果有搜索结果 只可能有一个结果
				User user = (User)criteria.list().get(0);
				getUserInfoBuilder.setResultCode(GetUserInfoMsg.GetUserInfoRsp.ResultCode.SUCCESS);
				
				UserData.UserItem.Builder userBuilder = UserData.UserItem.newBuilder();
				userBuilder.setUserId(user.getUserId());
				userBuilder.setUserName(user.getUserName());
				userBuilder.setHeadIndex(user.getHeadIndex());
				getUserInfoBuilder.setUserItem(userBuilder);
				
			}
			else{
				getUserInfoBuilder.setResultCode(GetUserInfoMsg.GetUserInfoRsp.ResultCode.USER_NOT_EXIST);
			}
			//回复客户端
			ServerNetwork.instance.sendMessageToClient(
					networkMessage.ioSession,
					NetworkMessage.packMessage(ProtoHead.ENetworkMessage.GETUSERINFO_RSP.getNumber(),
							networkMessage.getMessageID(), getUserInfoBuilder.build().toByteArray()));
			
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}

	/**
	 * 添加好友
	 * @param networkMessage
	 * @author wangfei
	 * @time 2015-03-24
	 */
	public void addFriend(NetworkMessage networkMessage){
		try {
			AddFriendMsg.AddFriendReq addFriendObject = AddFriendMsg.AddFriendReq.
					parseFrom(networkMessage.getMessageObjectBytes());
			AddFriendMsg.AddFriendRsp.Builder addFriendBuilder = AddFriendMsg.AddFriendRsp.newBuilder();
			
			ClientUser clientUser = ServerModel.instance.getClientUserFromTable(
					networkMessage.ioSession.getRemoteAddress().toString());
			try{
				//双方互加好友
				Session session = HibernateSessionFactory.getSession();
				Criteria criteria = session.createCriteria(User.class);
				Criteria criteria2 = session.createCriteria(User.class);
				criteria.add(Restrictions.eq("userId", clientUser.userId));
				User u = (User) criteria.list().get(0);
				criteria2.add(Restrictions.eq("userId", addFriendObject.getFriendUserId()));
				User friend = (User) criteria2.list().get(0);
				u.getFriends().add(friend);
				friend.getFriends().add(u);
			
				Transaction trans = session.beginTransaction();
			    session.update(u);
			    session.update(friend);
			    trans.commit();
			    
			    addFriendBuilder.setResultCode(AddFriendMsg.AddFriendRsp.ResultCode.SUCCESS);
			}catch(Exception e){
				addFriendBuilder.setResultCode(AddFriendMsg.AddFriendRsp.ResultCode.FAIL);
				e.printStackTrace();
			}
			

			//回复客户端
			ServerNetwork.instance.sendMessageToClient(
					networkMessage.ioSession,
					NetworkMessage.packMessage(ProtoHead.ENetworkMessage.ADDFRIEND_RSP.getNumber(),
							networkMessage.getMessageID(), addFriendBuilder.build().toByteArray()));
			
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 删除好友
	 * @param networkMessage
	 * @author wangfei
	 * @time 2015-03-24
	 */
	public void deleteFriend(NetworkMessage networkMessage){
		try {
			DeleteFriendMsg.DeleteFriendReq deleteFriendObject = DeleteFriendMsg.DeleteFriendReq.
					parseFrom(networkMessage.getMessageObjectBytes());
			DeleteFriendMsg.DeleteFriendRsp.Builder DeleteFriendBuilder = DeleteFriendMsg.DeleteFriendRsp.newBuilder();
			
			ClientUser clientUser = ServerModel.instance.getClientUserFromTable(
					networkMessage.ioSession.getRemoteAddress().toString());
			try{
				//双方互加好友
				Session session = HibernateSessionFactory.getSession();
				Criteria criteria = session.createCriteria(User.class);
				Criteria criteria2 = session.createCriteria(User.class);
				criteria.add(Restrictions.eq("userId", clientUser.userId));
				User u = (User) criteria.list().get(0);
				criteria2.add(Restrictions.eq("userId", deleteFriendObject.getFriendUserId()));
				User friend = (User) criteria2.list().get(0);
				User x=null,y=null;
				for(User a:u.getFriends()){
					if(a.getUserId().equals(friend.getUserId()))
						x=a;
				}
				for(User b:friend.getFriends()){
					if(b.getUserId().equals(u.getUserId()))
						y=b;
				}
				u.getFriends().remove(x);
				friend.getFriends().remove(y);
			
				Transaction trans = session.beginTransaction();
			    session.update(u);
			    session.update(friend);
			    trans.commit();
			    
			    DeleteFriendBuilder.setResultCode(DeleteFriendMsg.DeleteFriendRsp.ResultCode.SUCCESS);
			}catch(Exception e){
				DeleteFriendBuilder.setResultCode(DeleteFriendMsg.DeleteFriendRsp.ResultCode.FAIL);
				e.printStackTrace();
			}
			

			//回复客户端
			ServerNetwork.instance.sendMessageToClient(
					networkMessage.ioSession,
					NetworkMessage.packMessage(ProtoHead.ENetworkMessage.DELETEFRIEND_RSP.getNumber(),
							networkMessage.getMessageID(), DeleteFriendBuilder.build().toByteArray()));
			
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}