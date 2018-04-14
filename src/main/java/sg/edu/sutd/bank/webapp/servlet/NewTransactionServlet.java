/*
 * Copyright 2017 SUTD Licensed under the
	Educational Community License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may
	obtain a copy of the License at

https://opensource.org/licenses/ECL-2.0

	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an "AS IS"
	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
	or implied. See the License for the specific language governing
	permissions and limitations under the License.
 */

package sg.edu.sutd.bank.webapp.servlet;

import static sg.edu.sutd.bank.webapp.servlet.ServletPaths.NEW_TRANSACTION;
//import java.math.RoundingMode;

import java.io.IOException;
//ngpc - start
//import java.math.BigDecimal;
import java.math.*;
//ngpc - end

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import sg.edu.sutd.bank.webapp.commons.Constants;
import sg.edu.sutd.bank.webapp.commons.ServiceException;
//ngpc - start
import sg.edu.sutd.bank.webapp.model.ClientAccount;
import sg.edu.sutd.bank.webapp.model.ClientInfo;
//ngpc - end
import sg.edu.sutd.bank.webapp.model.ClientTransaction;
import sg.edu.sutd.bank.webapp.model.TransactionStatus;
import sg.edu.sutd.bank.webapp.model.User;
import sg.edu.sutd.bank.webapp.service.ClientInfoDAO;
import sg.edu.sutd.bank.webapp.service.ClientInfoDAOImpl;
import sg.edu.sutd.bank.webapp.service.ClientTransactionDAO;
import sg.edu.sutd.bank.webapp.service.ClientTransactionDAOImpl;
//ngpc -start
import sg.edu.sutd.bank.webapp.service.UserDAO;
import sg.edu.sutd.bank.webapp.service.UserDAOImpl;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAO;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAOImpl;
//ngpc -end

@WebServlet(NEW_TRANSACTION)
public class NewTransactionServlet extends DefaultServlet {
	private static final long serialVersionUID = 1L;
	//ngpc - start
	private ClientInfoDAO clientInforDao = new ClientInfoDAOImpl();
	private ClientAccount clientAccount_sdr = new ClientAccount();
	private ClientAccountDAO clientAcctDao = new ClientAccountDAOImpl();

	private ClientInfoDAO clientInforDao_rcv = new ClientInfoDAOImpl();
	private ClientAccount clientAccount_rcv = new ClientAccount();	
	//private ClientAccountDAO clientAcctDao_rcv = new ClientAccountDAOImpl();
	//ngpc - end
	private ClientTransactionDAO clientTransactionDAO = new ClientTransactionDAOImpl();
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			ClientTransaction clientTransaction = new ClientTransaction();
			//ClientTransaction clientTransaction_rcv = new ClientTransaction();
			
			User user = new User(getUserId(req));
			
			clientTransaction.setUser(user);
			clientTransaction.setAmount(new BigDecimal(req.getParameter("amount")));
			clientTransaction.setTransCode(req.getParameter("transcode"));
			clientTransaction.setToAccountNum(req.getParameter("toAccountNum"));
			//ngpc -start
			//TODO: change status to APPROVED or PENDING
			BigDecimal amt = new BigDecimal(req.getParameter("amount"));
			if(amt.compareTo(sg.edu.sutd.bank.webapp.commons.Constants.AMT_THRESHOLD) == -1) {
				clientTransaction.setStatus(TransactionStatus.APPROVED);
			}
			else {
				clientTransaction.setStatus(TransactionStatus.PENDING);
			}
			//ngpc - end
			//update DB!
			clientTransactionDAO.create(clientTransaction);

			//ngpc -start
			if(amt.compareTo(sg.edu.sutd.bank.webapp.commons.Constants.AMT_THRESHOLD) == -1) {
				// -1 means less than
				// 0 means equal in value
				// 1 means greater than
			//TODO: debit from sdr
				ClientInfo clientInfo = clientInforDao.loadAccountInfo(req.getRemoteUser());//.loadAccountInfo(req.getParameter("username"));
				BigDecimal prevBalance_sdr = clientInfo.getAccount().getAmount();
				BigDecimal nowBalance_sdr = new BigDecimal(0);//initialization
				nowBalance_sdr = prevBalance_sdr.subtract(clientTransaction.getAmount());
				//TODO: debit clientAccount_sdr object with -Amount before pushing into DAO for db UPDATE!
				clientAccount_sdr.setUser(user);
				clientAccount_sdr.setId(getUserId(req));
				clientAccount_sdr.setAmount(nowBalance_sdr);
				//TODO: update DB client_account.Amount with sdr New$Balance!
				clientAcctDao.update(clientAccount_sdr);
				
			//TODO: credit into rcv
				String rcv_id = req.getParameter("toAccountNum");
				UserDAO userDAO_rcv = new UserDAOImpl();
				User user_rcv = userDAO_rcv.loadUserByUserId(Integer.parseInt(rcv_id));
				
				ClientInfo clientInfo_rcv = clientInforDao_rcv.loadAccountInfo(user_rcv.getUserName());
				BigDecimal prevBalance_rcv = clientInfo_rcv.getAccount().getAmount();
				BigDecimal nowBalance_rcv = new BigDecimal(0);//initialization
				nowBalance_rcv = prevBalance_rcv.add(clientTransaction.getAmount());//from clientTransaction, NOT clientTransaction_rcv
				//TODO: credit clientAccount_sdr object with +Amount before pushing into DAO for db UPDATE!
				clientAccount_rcv.setUser(user_rcv);
				clientAccount_rcv.setId(Integer.parseInt(rcv_id));
				clientAccount_rcv.setAmount(nowBalance_rcv);
				//TODO: update DB client_account.Amount with rcv New$Balance!
				clientAcctDao.update(clientAccount_rcv);
			}
			//ngpc - end
			redirect(resp, ServletPaths.CLIENT_DASHBOARD_PAGE);

		} catch (ServiceException e) {
			sendError(req, e.getMessage());
			forward(req, resp);
		}
	}
}
