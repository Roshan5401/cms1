package com.canteen.controller;

import java.security.Principal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.canteen.entities.CanteenUsers;
import com.canteen.entities.OrderEntity;
import com.canteen.entities.menuCanteen;
import com.canteen.repository.CanteenUserRepository;
import com.canteen.repository.MenuRepository;
import com.canteen.repository.OrderRepository;
import com.canteen.service.OrderService;

@Controller
public class UserController {
	@Autowired
	CanteenUserRepository canteenUserRepository;
	
	@Autowired
	OrderService orderService;

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	OrderRepository orderRepository;
	
	@Autowired
	MenuRepository menuRepository;
	// Display bookOrder Page 	
	
	@GetMapping("/user/bookOrderByType")
	public String bookOrderByType(@ModelAttribute("type")String type, Model model , Principal principal) {
		System.out.println(type);
		String userName=principal.getName();
		CanteenUsers current_user=canteenUserRepository.findByEmail(userName);
		
		// Viewing Menu Backend Implemantation
		
		List<menuCanteen> foodItems=menuRepository.findAll();
		List<menuCanteen> enabledFoodItems  = foodItems.stream().filter(item->item.isEnable() == true).collect(Collectors.toList());
		Date date = new Date();
		@SuppressWarnings("deprecation")
		int month = date.getMonth();
		@SuppressWarnings("deprecation")
		List<menuCanteen> finalFoodItemsByType = enabledFoodItems.stream().filter(item->item.getFoodServedDate().getMonth()==month).collect(Collectors.toList());
		List<menuCanteen> temp = finalFoodItemsByType;
		
		if(!type.equals("All")) {
			finalFoodItemsByType = finalFoodItemsByType.stream().filter(item -> item.getType().equals(type)).collect(Collectors.toList());
		}
		
		
		//Upcoming Orders backend Implemantation
		int id = current_user.getId();
		List<OrderEntity> orders= this.orderService.getAllOrders("Booked");
		List<OrderEntity> userOrders = orders.stream().filter(order -> order.getCanteenUsers().getId() == id).collect(Collectors.toList());
		
		model.addAttribute("foodItems",finalFoodItemsByType);
		model.addAttribute("user",current_user);
		model.addAttribute("user_orders",userOrders);
		return "users/bookorder";
		
	}
	
	
	
	
	@GetMapping("/user/bookOrder")
	public String bookOrder(Model model,Principal principal) {
		String userName=principal.getName();
		CanteenUsers current_user=canteenUserRepository.findByEmail(userName);
		
		// Viewing Menu Backend Implemantation
		
		List<menuCanteen> foodItems=menuRepository.findAll();
		List<menuCanteen> enabledFoodItems  = foodItems.stream().filter(item->item.isEnable() == true).collect(Collectors.toList());
		Date date = new Date();
		@SuppressWarnings("deprecation")
		int month = date.getMonth();
		@SuppressWarnings("deprecation")
		List<menuCanteen> finalFoodItems = enabledFoodItems.stream().filter(item->item.getFoodServedDate().getMonth()==month).collect(Collectors.toList());
		
		//Upcoming Orders backend Implemantation
		int id = current_user.getId();
		List<OrderEntity> orders= this.orderService.getAllOrders("Booked");
		List<OrderEntity> userOrders = orders.stream().filter(order -> order.getCanteenUsers().getId() == id).collect(Collectors.toList());
		System.out.println(current_user.getWallet());
		model.addAttribute("foodItems",finalFoodItems);
		model.addAttribute("user",current_user);
		model.addAttribute("user_orders",userOrders);
		return "users/bookorder";
	}
	
	@GetMapping("user/cancelOrder/{Id}")
	public RedirectView cancelOrder(@PathVariable("Id")int id,Principal principal)
	{
		String userName=principal.getName();
		CanteenUsers current_user=canteenUserRepository.findByEmail(userName);
		Optional<OrderEntity> optional=orderRepository.findById(id);
		OrderEntity orderEntity=optional.get();
		
		Double d = current_user.getWallet() + orderEntity.getTotalPrice();
		DecimalFormat dfor = new DecimalFormat("0.00");
		String s = dfor.format(d);
		Double final_wallet = Double.valueOf(s);
		current_user.setWallet(final_wallet);
		canteenUserRepository.save(current_user);
		orderRepository.deleteById(id);
		return new RedirectView("/user/bookOrder");
	}
	
	// Display view previous orders page and it will display all the orders of current user in the page by default
	@GetMapping("/user/viewPreviousOrders")
	public String viewPreviousOrders(Model model,Principal principal) {
		String userName=principal.getName();
		CanteenUsers current_user=canteenUserRepository.findByEmail(userName);
		model.addAttribute("user",current_user);
		int id = current_user.getId();
		List<OrderEntity> orders= this.orderService.getAllOrders("Delivered");
		List<OrderEntity> finalOrders = orders.stream().filter(order -> order.getCanteenUsers().getId() == id).collect(Collectors.toList());
		
		model.addAttribute("userOrders",finalOrders);
		return "users/viewpreviousorders";
	}
	
	// It will handle view previous orders by date for current user and display viewpreviousorders page with filtered data 
		@GetMapping("/user/findAllOrdersByUserByDate")
		public String findAllOrdersByUserByData(@ModelAttribute("vieworderbydate")  String date, Principal principal , Model model) {
			String userName = principal.getName();
			CanteenUsers current_user = canteenUserRepository.findByEmail(userName);
			model.addAttribute("user", current_user);
			int id = current_user.getId();
			String userId = Integer.toString(id);
			LocalDate date1 = LocalDate.parse(date , DateTimeFormatter.ISO_DATE); 
			
			List<OrderEntity> orders = this.orderService.getAllOrdersByDateAndUserId(date1, userId, "Delivered");
			model.addAttribute("userOrders", orders);
			
			return "users/viewpreviousorders";
		}
	
	
	// display add money to wallet page with current wallet balance of current user
	@GetMapping("/user/addMoneyToWallet")
	public String addMoneyToWallet(Model model,Principal principal) {
		String userName=principal.getName();
		CanteenUsers current_user=canteenUserRepository.findByEmail(userName);
		model.addAttribute("user",current_user);
		return "users/addmoneytowallet";
	}
	
	// It will update current user wallet balance by taking input and again redirect to addmoneytowalletpage
		@GetMapping("/user/addBalance")
		public RedirectView addBalance(Model model,Principal principal,@ModelAttribute("amount") String amount,RedirectAttributes attributes) {
			String userName=principal.getName();
			
			Double d = Double.valueOf(amount);
			DecimalFormat defor = new DecimalFormat("0.00");
			String val = defor.format(d);
			Double finalVal = Double.valueOf(val);
			CanteenUsers current_user=canteenUserRepository.findByEmail(userName);
			Double oldBalance=current_user.getWallet();
			System.out.println(oldBalance);
			current_user.setWallet(oldBalance+finalVal);
			canteenUserRepository.save(current_user);
			model.addAttribute("user",current_user);
			attributes.addAttribute("success",1);
			return new RedirectView("/user/addMoneyToWallet");
		}
	
	
	// display update profile page
	@GetMapping("/user/updateProfile")
	public String updateProfile(Model model,Principal principal) {
		String userName=principal.getName();
		CanteenUsers current_user=canteenUserRepository.findByEmail(userName);
		System.out.println(current_user.getEmail());
		model.addAttribute("user",current_user);
		model.addAttribute("update_user",new CanteenUsers());
		return "users/updateprofile";
	}
	// Save Changes Button Handler - it will update the user data and again return the same page
	@PostMapping("/user/updateUserProfile")
	public RedirectView updateUserProfile(Model model,Principal principal,@ModelAttribute("oldpassword")String oldPassword,@ModelAttribute("newpassword")String newPasword,@ModelAttribute("update_user")CanteenUsers users,RedirectAttributes attributes) {
		String userName=principal.getName();
		CanteenUsers current_user=canteenUserRepository.findByEmail(userName);
		current_user.setName(users.getName());
		current_user.setPhone(users.getPhone());
		if(oldPassword.length()>0 && newPasword.length()>0)
		{
			if(bCryptPasswordEncoder.matches(oldPassword, current_user.getPassword()))
			{
				current_user.setPassword(bCryptPasswordEncoder.encode(newPasword));
				attributes.addAttribute("success","1");
				return new RedirectView("/user/updateProfile");
			}
			else
			{
				attributes.addAttribute("error","1");
				return new RedirectView("/user/updateProfile");
			}
		}
		canteenUserRepository.save(current_user);
		return new RedirectView("/user/updateProfile");
	}
	@GetMapping("/user/submitfeedback/{ID}")
	public String submitFeedback(Model model,@PathVariable("ID") Integer Id ) {
		 Optional <OrderEntity> optional=orderRepository.findById(Id);
		 OrderEntity orderEntity=optional.get();
		 model.addAttribute("order",orderEntity);
		 return "/users/submitfeedback";
	}
	@GetMapping("/user/savefeedback/{ID}")
	public RedirectView saveFeedback(@PathVariable("ID") Integer Id ,@ModelAttribute("feedbackdata") String feedback)
	{
		Optional <OrderEntity> optional=orderRepository.findById(Id);
		OrderEntity orderEntity=optional.get();
		System.out.println(Id);
		orderEntity.setFeedback(feedback);
		orderRepository.save(orderEntity);
		return  new RedirectView("/user/viewPreviousOrders");
	}
	TreeMap<LocalDate,Integer> treeMap=new TreeMap<>();
	double  ordersTotal = 0;
	@GetMapping("/user/selectDates/{foodId}/{foodServedDate}")
	public String selectDates( Principal principal ,Model model, @PathVariable("foodId")String foodId, @PathVariable("foodServedDate")String foodServedDate)
	{
		treeMap.clear();
		ordersTotal = 0;
		int id = Integer.parseInt(foodId);
		menuCanteen food = menuRepository.findById(id);
		model.addAttribute("treeMap",treeMap);
		model.addAttribute("food", food);
		//Todays Date
		LocalDate today = LocalDate.now();
		int day = today.getDayOfMonth();
		//Food Served date in localdate formate for cmparision
		LocalDate servedDate = LocalDate.parse(foodServedDate , DateTimeFormatter.ISO_DATE);
		int servedDay = servedDate.getDayOfMonth();
		
		String email = principal.getName();
		CanteenUsers canteenUser = canteenUserRepository.findByEmail(email);
		model.addAttribute("user", canteenUser);
		model.addAttribute("ordersTotal", ordersTotal);
		
		return "/users/selectdates";
	}
	
	@GetMapping("user/addDateOfOrder")
	public String addDateOfOrder( Principal principal ,Model model,@ModelAttribute("dateinput")String date,@ModelAttribute("quantityinput")Integer quantity, @ModelAttribute("foodId") String foodId, @ModelAttribute("foodServedDate")String foodServedDate) throws ParseException
	{
		System.out.println(date);
//		System.out.println(quantity);
		LocalDate requestedDate = LocalDate.parse(date , DateTimeFormatter.ISO_DATE); 
		LocalDate foodservedDate = LocalDate.parse(foodServedDate, DateTimeFormatter.ISO_DATE);
		
		// Check if user is ordering for previous days or next month
		LocalDate today = LocalDate.now();
		Month currentMonth = today.getMonth();
		int currentDay = today.getDayOfMonth();
		
		Month requestedMonth = requestedDate.getMonth();
		int requestedDay = requestedDate.getDayOfMonth();
		
		int servedDay = foodservedDate.getDayOfMonth();
		
		DayOfWeek requestedDayOfWeek = requestedDate.getDayOfWeek();
		String day = requestedDayOfWeek.toString();
		
		
		
		System.out.println(requestedDay);
		System.out.println(servedDay);
		
		int id = Integer.parseInt(foodId);
		menuCanteen food = menuRepository.findById(id);
		
		
		// Check if it is not 3:00 pm already
		
		LocalTime time1 , time2;
		 LocalTime localTime = LocalTime.now();
		 
		 time1 = LocalTime.of(localTime.getHour(), localTime.getMinute(), localTime.getMinute());
		 time2 = LocalTime.of(15,00,00);
		 
		
		
		if(requestedMonth == currentMonth && requestedDay >= currentDay &&  requestedDay >= servedDay  && (!day.equals("SATURDAY"))  && (!day.equals("SUNDAY"))) {
			 if(requestedDay == currentDay && time1.compareTo(time2) > 0 ) {
				 	
			 }
			 else {
				 treeMap.put(requestedDate, quantity);
					ordersTotal += (quantity*(food.getPrice()));
			 }
			
		}
		
		model.addAttribute("food", food);
		model.addAttribute("treeMap",treeMap);
		model.addAttribute("ordersTotal",ordersTotal);
		
		String email = principal.getName();
		CanteenUsers canteenUser = canteenUserRepository.findByEmail(email);
		
		model.addAttribute("user",canteenUser);
		
		return "/users/selectdates"; 
//		return new RedirectView("/user/selectDates/"+foodId+"/"+foodServedDate);
	}
	@GetMapping("/user/deleteDates/{Date}/{foodId}")
	public String deleteDates(Principal principal ,Model model,@PathVariable("Date")String Date,@PathVariable("foodId") String foodId )
	{
		LocalDate date1 = LocalDate.parse(Date , DateTimeFormatter.ISO_DATE);

		int id = Integer.parseInt(foodId);
		menuCanteen food = menuRepository.findById(id);
		model.addAttribute("food", food);
		String email = principal.getName();
		CanteenUsers canteenUser = canteenUserRepository.findByEmail(email);
		int val = treeMap.get(date1);
		ordersTotal -= (val * food.getPrice());
		model.addAttribute("ordersTotal",ordersTotal);
		model.addAttribute("user",canteenUser);
		
		treeMap.remove(date1);
		model.addAttribute("treeMap",treeMap);
		return "/users/selectdates";
	}
	@GetMapping("/user/addDate/{foodId}")
	public String addDate(Principal principal,Model model, @PathVariable("foodId")String foodId) {
		model.addAttribute("treeMap", treeMap);
		int id = Integer.parseInt(foodId);
		menuCanteen food = menuRepository.findById(id);
		model.addAttribute("food", food);
		String email = principal.getName();
		CanteenUsers canteenUser = canteenUserRepository.findByEmail(email);
		model.addAttribute("ordersTotal", ordersTotal);
		model.addAttribute("user",canteenUser);
		
		
		
		return "/users/selectdates";
	}
	
	@GetMapping("/user/updateQuantityOfOrder")
	public RedirectView updateQuantityOfOrder(Model model,@ModelAttribute("selecteddate")String date , @ModelAttribute("selectedquantity")String quantity, @ModelAttribute("foodId") String foodId, @ModelAttribute("price") String price) {
		LocalDate date1 = LocalDate.parse(date , DateTimeFormatter.ISO_DATE);
		int quant = Integer.parseInt(quantity);
		int prevQuantity = treeMap.get(date1);
//		int foodPrice = Integer.parseInt(price);
		int id = Integer.parseInt(foodId);
		
		menuCanteen food = menuRepository.findById(id);
		
		if(quant < prevQuantity) {
			ordersTotal -= ((prevQuantity - quant) * food.getPrice());
		}
		else {
			ordersTotal+=((quant-prevQuantity) * food.getPrice());
		}
		treeMap.put(date1 , quant);
		return new RedirectView("/user/addDate/"+foodId);
	}
	
	@GetMapping("/user/confirmOrder/{foodId}")
	public RedirectView confirmOrder(Principal principal, @PathVariable("foodId")String foodId) {
		int id = Integer.parseInt(foodId);
		menuCanteen food = menuRepository.findById(id);
		String email = principal.getName();
		CanteenUsers canteenUser = canteenUserRepository.findByEmail(email);
		
		
		
		
		  // getting keySet() into Set
        Set<LocalDate> set1 = treeMap.keySet();
 
        // Book only if orders Total is less than user wallet balance
       
        
        Double walletTotal = canteenUser.getWallet();
        
        if(ordersTotal <= walletTotal) {
        	// Reduce the wallet balance of the user first then update the user walllet in db
        	// Then book orders
        	
        	Double d = walletTotal-ordersTotal;
        	DecimalFormat dfor = new DecimalFormat("0.00");
        	String s = dfor.format(d);
        	Double finalWallet = Double.valueOf(s);
        	
        	canteenUser.setWallet(finalWallet);
        	canteenUserRepository.save(canteenUser);
        	
        	
        	// for-each loop to book order for all dates
            for(LocalDate key : set1) {
            	OrderEntity order = new OrderEntity();
        		order.setCanteenUsers(canteenUser);
        		order.setFood(food);
        		
            	order.setOrderDate( java.sql.Date.valueOf(key));
            	order.setQuantity(treeMap.get(key));
            	order.setStatus("Booked");
            	order.setTotalPrice(treeMap.get(key) * food.getPrice());
            	
            	orderRepository.save(order);
            }
        	
        }
        
        else {
        	return new RedirectView("/user/addDate/"+foodId);
        }
        
        
		
		return new RedirectView("/user/bookOrder");
	}
	
	
}