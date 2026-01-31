To Do: 
- Add the ability to request refunds through the plans portal
    - When and administrator needs to request a refund, they should be able to go to the plans page, select the payment plan, enter how much to refund, provide a justification, then submit
    - Once submitted, the app should automatically send an email or emails to those defined as accounting approvers. 
        - The approvers will recieve an email that will provide them with a link. That link will take them to a unique page where they can either accept or deny the refund request. If denied, a justification should be given. If Accepted, the app should automatically work with square to process the refund. 

    TODO: Implement email notification system (e.g., JavaMailSender) in the backend. When a refund is requested, send an email to accounting approvers with a unique approval/denial link. The link should point to an endpoint or UI for the approver to act on the request. This logic is not yet present in the codebase.

    Step-by-step plan for refund approval email/notification system:
    1. Add a mail-sending service to the backend (e.g., using JavaMailSender).
    2. Store or configure a list of accounting approver emails (in DB or config).
    3. When a refund is requested, generate a unique approval/denial link (with a secure token or refund ID).
    4. Send an email to all approvers with refund details and the approval/denial link.
    5. Create endpoints/UI for approvers to approve or deny the refund via the link.
    6. On approval/denial, update the refund status and notify the requester if needed.
- Add the ability to see all transactions that have occured that are being applied towards the specific plan
    - When the plan ID is clicked, the app should open a model that will show detailed information about the payment plan. 
        - The items I would like to see in the model includes the following
            - Student ID
            - Email
            - Plan start date
            - Plan end date (Estimated)
            - Button to change payment method
            - Button to request refund
            - Status
            - Button to cancel plan
            - Total Due
            - Amount Paid
            - Frequency of Payments
            - Next Payment Date
            - List of all transactions that have been applied to this plan
                - Transaction ID
                - Date of Transaction
                - Amount
                - Payment Method
                - Status (Paid, Refunded, Failed, etc)
                - Button to view receipt
        - The model should allow for editing things such as the email address, frequency of payments, and amount per payment
- The create new plan feature needs to be modified to charge the fees back to the customer like it does on the /pay page. 
- Create an enviromental variable that can be set causing a banner to appear across the header of each page denoting the instance is a test/dev enviroment. 