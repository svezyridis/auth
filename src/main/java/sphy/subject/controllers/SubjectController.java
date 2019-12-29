package sphy.subject.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import sphy.RestResponse;
import sphy.Validator;
import sphy.auth.models.NewUser;
import sphy.subject.db.CategoryRepository;
import sphy.subject.models.*;
import sphy.subject.db.SubjectRepository;


import java.util.List;


@RestController
public class SubjectController {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("jdbcSubjectRepository")
    private SubjectRepository subjectRepository;

    @Autowired
    @Qualifier("jdbcCategoryRepository")
    private CategoryRepository categoryRepository;

    @Autowired
    Validator validator;


    /**
     * @param category name of the requested category
     * @param token
     * @return all subjects of the specified category
     */
    @RequestMapping(value = "subject/{weapon}/{category}")
    public RestResponse getSubjectByCategory(@PathVariable String category, @PathVariable String weapon, @RequestHeader("authorization") String token) {
        if (!validator.simpleValidateToken(token))
            return new RestResponse("error", null, "token is invalid");

        Integer weaponID = categoryRepository.getWeaponID(weapon);
        if (weaponID == -1)
            return new RestResponse("error", null, "weapon does not exist");

        Integer categoryID = categoryRepository.getCategoryID(category, weaponID);
        if (categoryID == -1)
            return new RestResponse("error", null, "category does not exist");

        List<Subject> subjects = subjectRepository.getSubjectsOfCategory(categoryID);
        for (Subject sub : subjects) {
            List<Image> images = subjectRepository.getImagesOfSubject(sub.getID());
            sub.setImages(images);
            sub.setCategory(category);
        }
        return new RestResponse("success", subjects, null);
    }

    /**
     * Creates a new subject
     *
     * @param token JWT token
     * @param weapon
     * @param category
     * @param newSubject Json body of the form:
     *                     newSubject:{
     *                     name,
     *                     text
     *                     }
     * @return error message if any success with the new subject created otherwise
     */
    @PostMapping(value = "subject/{weapon}/{category}")
    public RestResponse createSubject(@RequestHeader("authorization") String token, @PathVariable String weapon, @PathVariable String category, @RequestBody NewSubject newSubject) {
        if (!validator.validateAdminToken(token))
            return new RestResponse("error", null, "token is not a valid ADMIN token");
        Subject subjectToAdd = newSubject.getNewSubject();
        Integer weaponID = categoryRepository.getWeaponID(weapon);
        if (weaponID == -1)
            return new RestResponse("error", null, "weapon does not exist");

        Integer categoryID = categoryRepository.getCategoryID(category, weaponID);
        if (categoryID == -1)
            return new RestResponse("error", null, "category does not exist");

        Integer subjectID = subjectRepository.getSubjectID(subjectToAdd.getName());
        if (subjectID != -1)
            return new RestResponse("error", null, "subject already exists");

        int res = subjectRepository.createSubject(subjectToAdd, categoryID);
        if (res == 0)
            return new RestResponse("error", null, "subject creation failed");

        subjectToAdd.setCategoryID(categoryID);
        return new RestResponse("success", subjectToAdd, null);
    }

    @DeleteMapping(value = "subject/{weapon}/{category}/{subject}")
    public RestResponse deleteSubject(@RequestHeader("authorization") String token, @PathVariable String weapon, @PathVariable String category, @PathVariable String subject){
        if (!validator.validateAdminToken(token))
            return new RestResponse("error", null, "token is not a valid ADMIN token");
        Integer weaponID = categoryRepository.getWeaponID(weapon);
        if (weaponID == -1)
            return new RestResponse("error", null, "weapon does not exist");

        Integer categoryID = categoryRepository.getCategoryID(category, weaponID);
        if (categoryID == -1)
            return new RestResponse("error", null, "category does not exist");

        Integer subjectID = subjectRepository.getSubjectID(subject);
        if (subjectID == -1)
            return new RestResponse("error", null, "subject does not exist");
        Integer result=subjectRepository.deleteSubject(subjectID);
        if(result==-1)
            return new RestResponse("error", null, "subject could not be deleted");
        return new RestResponse("success", null, "subject deleted successfully");
    }


}