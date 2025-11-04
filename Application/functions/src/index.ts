// functions/src/index.ts (The FINAL, SECURE, and DELAYED INIT CODE)

import * as functions from "firebase-functions";
import { v2 as cloudinary } from "cloudinary";
import { CallableRequest } from "firebase-functions/v2/https";

// Define the expected structure for the data argument
interface SignatureRequestData {
  folder?: string;
}

// Define the Callable Function
export const generateCloudinarySignature = functions.https.onCall(
  async (request: CallableRequest<SignatureRequestData>) => {

    // 1. Authentication Check
    if (!request.auth) {
      throw new functions.https.HttpsError("unauthenticated", "The request requires user authentication.");
    }
    
    // ----------------------------------------------------------------------
    // CRITICAL FIX: Read config and initialize Cloudinary inside the function call
    // This delays execution until the function is running (not just deploying/analyzing)
    
    // 1. READ SECURE CONFIGURATION
    const config = functions.config().cloudinary!; 

    // 2. CONFIGURE CLOUDINARY
    cloudinary.config({
      cloud_name: config.cloud_name, 
      api_key: config.api_key,       
      api_secret: config.api_secret, 
    });
    // ----------------------------------------------------------------------

    // 2. Define Upload Parameters
    const folder = request.data.folder || "misc";
    const timestamp = Math.round(new Date().getTime() / 1000);

    const params = {
      timestamp: timestamp,
      folder: folder,
    };

    // 3. Generate the Secure Signature using the hidden API Secret
    const signature = cloudinary.utils.api_sign_request(params, config.api_secret);

    // 4. Return the necessary data to the Android App
    return {
      signature: signature,
      timestamp: timestamp,
      cloudName: config.cloud_name,
      apiKey: config.api_key,
      folder: folder,
    };
  }
);