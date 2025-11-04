// functions/src/index.ts (V2 SECRETS FIX)

import { onCall, CallableRequest } from "firebase-functions/v2/https";
import * as functions from "firebase-functions/v2";
import { v2 as cloudinary } from "cloudinary";
import { defineSecret } from "firebase-functions/params"

// Define the expected structure for the data argument
interface SignatureRequestData {
    folder?: string;
}

// ----------------------------------------------------------------------
// 1. Define Secrets - These link to the values you set in Step 1
// ----------------------------------------------------------------------
const CLOUDINARY_CLOUD_NAME_SECRET = defineSecret("CLOUDINARY_CLOUD_NAME");
const CLOUDINARY_API_KEY_SECRET = defineSecret("CLOUDINARY_API_KEY");
const CLOUDINARY_API_SECRET_SECRET = defineSecret("CLOUDINARY_API_SECRET");


// Define the V2 Callable Function
// 2. Link the secrets to the function using the 'secrets' option
export const generateCloudinarySignature = onCall(
    { secrets: [CLOUDINARY_CLOUD_NAME_SECRET, CLOUDINARY_API_KEY_SECRET, CLOUDINARY_API_SECRET_SECRET] },
    async (request: CallableRequest<SignatureRequestData>) => {

        // 3. Initialize Cloudinary INSIDE the function (where secrets are available)
        cloudinary.config({
            cloud_name: CLOUDINARY_CLOUD_NAME_SECRET.value(), // Access value using .value()
            api_key: CLOUDINARY_API_KEY_SECRET.value(),       // Access value using .value()
            api_secret: CLOUDINARY_API_SECRET_SECRET.value(), // Access value using .value()
        });

        // 1. Authentication Check
        if (!request.auth) {
            throw new functions.https.HttpsError("unauthenticated", "The request requires user authentication.");
        }
        
        // Safety Check: Check for the value existence after config
        if (!CLOUDINARY_API_SECRET_SECRET.value()) {
            throw new functions.https.HttpsError("internal", "Cloudinary secret not loaded."); 
        }
        
        // 2. Define Upload Parameters
        const folder = request.data.folder || "misc";
        const timestamp = Math.round(new Date().getTime() / 1000);

        const params = {
            timestamp: timestamp,
            folder: folder,
        };
        
        // 3. Generate the Secure Signature
        const signature = cloudinary.utils.api_sign_request(params, CLOUDINARY_API_SECRET_SECRET.value());

        // 4. Return the necessary data to the Android App
        return {
            signature: signature,
            timestamp: timestamp,
            cloudName: CLOUDINARY_CLOUD_NAME_SECRET.value(),
            apiKey: CLOUDINARY_API_KEY_SECRET.value(),
            folder: folder,
        };
    }
);